package com.banktransactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@SuppressWarnings({"DataFlowIssue", "BusyWait"})
class ImportIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    @LocalServerPort
    int port;

    private final RestTemplate restTemplate = noErrorRestTemplate();

    // ── UC1: Poprawny import ──────────────────────────────────────────────────

    @Test
    void shouldReturn201WithJobIdAfterUpload() {
        ResponseEntity<Map<String, Object>> response = uploadCsv("test-transactions.csv");
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(Objects.requireNonNull(response.getBody())).containsKey("id");
        assertThat(response.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void shouldReturnPendingOrProcessingRightAfterUpload() {
        Map<String, Object> body = Objects.requireNonNull(uploadCsv("test-transactions.csv").getBody());
        assertThat((String) body.get("status")).isIn("PENDING", "PROCESSING");
    }

    @Test
    void shouldEventuallyCompleteImport() throws InterruptedException {
        String jobId = (String) Objects.requireNonNull(uploadCsv("test-transactions.csv").getBody()).get("id");
        String finalStatus = pollUntilDone(jobId, 10_000);
        assertThat(finalStatus).isIn("COMPLETED", "COMPLETED_WITH_ERRORS");
    }

    // ── UC2: Status importu z błędami walidacji ───────────────────────────────

    @Test
    void shouldReportRowLevelValidationErrors() throws InterruptedException {
        String jobId = (String) Objects.requireNonNull(uploadCsv("test-transactions.csv").getBody()).get("id");
        pollUntilDone(jobId, 10_000);

        Map<String, Object> body = Objects.requireNonNull(get("/api/v1/imports/" + jobId).getBody());
        assertThat((Integer) body.get("invalidRows")).isGreaterThan(0);
        assertThat(body.get("errors")).isNotNull();
    }

    @Test
    void shouldReturn404ForUnknownJob() {
        assertThat(get("/api/v1/imports/no-such-id").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldReturn404WithProblemDetailForUnknownJob() {
        ResponseEntity<Map<String, Object>> response = get("/api/v1/imports/no-such-id");
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(Objects.requireNonNull(response.getBody())).containsKey("detail");
        assertThat(response.getBody()).containsEntry("errorCode", "import-job-not-found");
    }

    // ── UC2d: Lista importów ──────────────────────────────────────────────────

    @Test
    void shouldReturnNonEmptyListAfterImport() throws InterruptedException {
        uploadCsv("test-transactions.csv");

        ResponseEntity<Object[]> response = restTemplate.getForEntity(url("/api/v1/imports"), Object[].class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Objects.requireNonNull(response.getBody())).isNotEmpty();
    }

    // ── UC3: Statystyki ───────────────────────────────────────────────────────

    @Test
    void shouldReturnStatisticsGroupedByCategory() throws InterruptedException {
        String jobId = (String) Objects.requireNonNull(uploadCsv("test-transactions.csv").getBody()).get("id");
        pollUntilDone(jobId, 10_000);

        ResponseEntity<Object[]> resp = restTemplate.getForEntity(url("/api/v1/statistics?groupBy=CATEGORY"), Object[].class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotEmpty();
    }

    @Test
    void shouldReturn400WithProblemDetailForInvalidGroupBy() {
        ResponseEntity<Map<String, Object>> response = get("/api/v1/statistics?groupBy=invalid");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(Objects.requireNonNull(response.getBody())).containsKey("detail");
        assertThat(Objects.requireNonNull(response.getBody())).containsEntry("errorCode", "bad-request");
    }

    // ── Obsługa błędów: złe rozszerzenie (415) ───────────────────────────────

    @Test
    void shouldReturn415WhenFileHasWrongExtension() {
        ResponseEntity<Map<String, Object>> response = uploadFile("wrong-extension.txt", "wrong-extension.txt");
        assertThat(response.getStatusCode().value()).isEqualTo(415);
        assertThat(Objects.requireNonNull(response.getBody())).containsEntry("errorCode", "unsupported-file-type");
    }

    // ── Obsługa błędów: zła struktura CSV (job FAILED) ───────────────────────

    @Test
    void shouldFailJobWhenCsvHasWrongColumnStructure() throws InterruptedException {
        ResponseEntity<Map<String, Object>> response = uploadCsv("wrong-structure.csv");
        assertThat(response.getStatusCode().value()).isEqualTo(201);

        String jobId = (String) Objects.requireNonNull(response.getBody()).get("id");
        String status = pollUntilDone(jobId, 10_000);
        assertThat(status).isEqualTo("FAILED");

        Map<String, Object> jobBody = Objects.requireNonNull(get("/api/v1/imports/" + jobId).getBody());
        assertThat(jobBody.get("errors").toString()).containsAnyOf("transactionId", "iban", "transactionDate");
    }

    // ── Obsługa błędów: plik z wieloma błędnymi wierszami ────────────────────

    @Test
    void shouldHandleFileWithMultipleInvalidEntries() throws InterruptedException {
        String jobId = (String) Objects.requireNonNull(uploadCsv("invalid-entries.csv").getBody()).get("id");
        pollUntilDone(jobId, 10_000);

        Map<String, Object> body = Objects.requireNonNull(get("/api/v1/imports/" + jobId).getBody());
        assertThat(body.get("status")).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat((Integer) body.get("invalidRows")).isGreaterThanOrEqualTo(5);
        assertThat((Integer) body.get("validRows")).isGreaterThan(0);
    }

    // ── Obsługa błędów: plik z >10000 wierszy (job FAILED) ───────────────────

    @Test
    @SuppressWarnings("unchecked")
    void shouldFailJobWhenCsvExceeds10000Rows(@TempDir Path tempDir) throws Exception {
        Path largeFile = generateLargeCsv(tempDir, 10_001);
        ResponseEntity<Map<String, Object>> response = postFile(largeFile);
        assertThat(response.getStatusCode().value()).isEqualTo(201);

        String jobId = (String) Objects.requireNonNull(response.getBody()).get("id");
        String status = pollUntilDone(jobId, 20_000);
        assertThat(status).isEqualTo("FAILED");

        Map<String, Object> jobBody = Objects.requireNonNull(get("/api/v1/imports/" + jobId).getBody());
        assertThat(jobBody.get("errors").toString()).contains("10000");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAcceptCsvWithExactly10000Rows(@TempDir Path tempDir) throws Exception {
        Path file = generateLargeCsv(tempDir, 10_000);
        ResponseEntity<Map<String, Object>> response = postFile(file);
        assertThat(response.getStatusCode().value()).isEqualTo(201);

        String jobId = (String) Objects.requireNonNull(response.getBody()).get("id");
        String status = pollUntilDone(jobId, 120_000);
        assertThat(status).isIn("COMPLETED", "COMPLETED_WITH_ERRORS");

        Map<String, Object> jobBody = Objects.requireNonNull(get("/api/v1/imports/" + jobId).getBody());
        assertThat((Integer) jobBody.get("processedRows")).isEqualTo(10_000);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> uploadCsv(String filename) {
        return uploadFile(filename, filename);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> uploadFile(String resourceName, String submittedFilename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new RenamedResource(new ClassPathResource(resourceName), submittedFilename));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.postForEntity(
                url("/api/v1/imports"),
                new HttpEntity<>(body, headers),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> postFile(Path file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.postForEntity(
                url("/api/v1/imports"),
                new HttpEntity<>(body, headers),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> get(String path) {
        return restTemplate.getForEntity(url(path), (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    private String pollUntilDone(String jobId, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String status = (String) Objects.requireNonNull(get("/api/v1/imports/" + jobId).getBody()).get("status");
            if ("COMPLETED".equals(status) || "COMPLETED_WITH_ERRORS".equals(status)
                    || "FAILED".equals(status)) {
                return status;
            }
            //noinspection BusyWait
            Thread.sleep(300);
        }
        throw new AssertionError("Import job did not finish within " + timeoutMs + "ms");
    }

    private Path generateLargeCsv(Path dir, int rows) throws IOException {
        Path file = dir.resolve("large-" + rows + ".csv");
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("transactionId,iban,transactionDate,currency,category,amount\n");
            for (int i = 0; i < rows; i++) {
                w.write(String.format("gen-%08d-0000-0000-0000-000000000000,PL61109010140000071219812874,2024-01-15,PLN,GROCERIES,100.00\n", i));
            }
        }
        return file;
    }

    private String url(String path) { return "http://localhost:" + port + path; }

    private static RestTemplate noErrorRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new ResponseErrorHandler() {
            @Override @SuppressWarnings("NullableProblems")
            public boolean hasError(ClientHttpResponse r) { return false; }
            @Override @SuppressWarnings("NullableProblems")
            public void handleError(URI url, HttpMethod method, ClientHttpResponse r) {}
        });
        return rt;
    }

    private static class RenamedResource extends org.springframework.core.io.AbstractResource {
        private final org.springframework.core.io.Resource delegate;
        private final String filename;

        RenamedResource(org.springframework.core.io.Resource delegate, String filename) {
            this.delegate = delegate;
            this.filename = filename;
        }

        @Override public String getFilename() { return filename; }
        @Override @SuppressWarnings("NullableProblems")
        public String getDescription() { return delegate.getDescription(); }
        @Override @SuppressWarnings("NullableProblems")
        public java.io.InputStream getInputStream() throws IOException { return delegate.getInputStream(); }
    }
}



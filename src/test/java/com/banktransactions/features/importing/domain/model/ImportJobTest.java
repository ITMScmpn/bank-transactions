package com.banktransactions.features.importing.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ImportJobTest {

    @Test
    void shouldStartAsPending() {
        ImportJob job = newJob();
        assertThat(job.getStatus()).isEqualTo(ImportStatus.PENDING);
        assertThat(job.getValidRows()).isZero();
        assertThat(job.getInvalidRows()).isZero();
    }

    @Test
    void shouldTransitionToProcessing() {
        ImportJob job = newJob();
        job.startProcessing();
        assertThat(job.getStatus()).isEqualTo(ImportStatus.PROCESSING);
    }

    @Test
    void shouldNotAllowStartProcessingTwice() {
        ImportJob job = newJob();
        job.startProcessing();
        assertThatThrownBy(job::startProcessing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void shouldCompleteWithoutErrors() {
        ImportJob job = newJob();
        job.startProcessing();
        job.recordValid();
        job.recordValid();
        job.complete();
        assertThat(job.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(job.getValidRows()).isEqualTo(2);
        assertThat(job.getProcessedRows()).isEqualTo(2);
    }

    @Test
    void shouldCompleteWithErrors() {
        ImportJob job = newJob();
        job.startProcessing();
        job.recordValid();
        job.recordInvalid(new TransactionError(2, "bad,row", "Invalid IBAN format"));
        job.complete();
        assertThat(job.getStatus()).isEqualTo(ImportStatus.COMPLETED_WITH_ERRORS);
        assertThat(job.getValidRows()).isEqualTo(1);
        assertThat(job.getInvalidRows()).isEqualTo(1);
        assertThat(job.getErrors()).hasSize(1);
        assertThat(job.getErrors().getFirst().errorMessage()).isEqualTo("Invalid IBAN format");
    }

    @Test
    void shouldFail() {
        ImportJob job = newJob();
        job.startProcessing();
        job.fail("Critical error");
        assertThat(job.getStatus()).isEqualTo(ImportStatus.FAILED);
    }

    @Test
    void shouldReturnUnmodifiableErrorsList() {
        ImportJob job = newJob();
        assertThatThrownBy(() -> job.getErrors().add(new TransactionError(1, "", "x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private ImportJob newJob() {
        return new ImportJob("id1", "test.csv", LocalDateTime.now());
    }
}

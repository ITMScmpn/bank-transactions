package com.banktransactions.features.importing.adapter.worker;

import com.banktransactions.features.importing.application.port.out.AsyncImportTriggerPort;
import com.banktransactions.features.importing.application.service.ProcessImportJobService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Adapter wejściowy dla asynchronicznego przetwarzania.
 * Implementuje AsyncImportTriggerPort — warstwa aplikacyjna zależy od portu,
 * nie bezpośrednio od tego adaptera (poprawna architektura heksagonalna).
 */
@Component
public class ImportJobWorker implements AsyncImportTriggerPort {

    private final ProcessImportJobService processImportJobService;

    public ImportJobWorker(ProcessImportJobService processImportJobService) {
        this.processImportJobService = processImportJobService;
    }

    @Async("importTaskExecutor")
    @Override
    public void trigger(String jobId, InputStream csvInputStream) {
        processImportJobService.process(jobId, csvInputStream);
    }
}

package com.banktransactions.features.importing.application.port.out;

import java.io.InputStream;

/**
 * Port wyjściowy wyzwalający asynchroniczne przetwarzanie importu.
 * Warstwa aplikacyjna nie powinna wiedzieć, jak uruchamiane jest async —
 * implementacja w adapterze worker (Spring @Async).
 */
public interface AsyncImportTriggerPort {
    void trigger(String jobId, InputStream csvInputStream);
}


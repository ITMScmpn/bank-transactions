# Bank Transactions — Import & Analytics API

REST API do importu wyciągów bankowych (CSV) i agregacji statystyk budżetowych per kategoria, IBAN lub miesiąc.  
Architektura heksagonalna (Ports & Adapters) · Spring Boot 4 · Java 21 · MongoDB

---

## Spis treści

1. [Co zostało zrealizowane](#1-co-zostało-zrealizowane)
2. [Założenie: UUID transakcji](#2-założenie-uuid-transakcji)
3. [Szybki start z Dockerem](#3-szybki-start-z-dockerem)
4. [Format CSV](#4-format-csv)
5. [Endpointy API](#5-endpointy-api)
6. [Testowanie](#6-testowanie)
7. [Architektura](#7-architektura)
8. [Eksploracja domeny bankowej](#8-eksploracja-domeny-bankowej)
9. [Propozycje rozwoju](#9-propozycje-rozwoju)

---

## 1. Co zostało zrealizowane

### Funkcjonalności

| Wymaganie | Status | Uwagi |
|-----------|--------|-------|
| Import pliku CSV z transakcjami | ✅ | `POST /api/v1/imports` |
| Przetwarzanie asynchroniczne | ✅ | `@Async`, odpowiedź 201 natychmiastowa |
| Śledzenie statusu importu | ✅ | `GET /api/v1/imports/{id}` — stany PENDING → PROCESSING → COMPLETED / FAILED |
| Walidacja per wiersz | ✅ | IBAN, data, waluta, kategoria, kwota; błędne wiersze raportowane w `errors[]` |
| Obsługa 10 000 wierszy | ✅ | Limit konfigurowalny; wstawianie partiami po 500 rekordów |
| Statystyki per kategoria | ✅ | `GET /api/v1/statistics?groupBy=CATEGORY` |
| Statystyki per IBAN | ✅ | `GET /api/v1/statistics?groupBy=IBAN` |
| Statystyki per miesiąc | ✅ | `GET /api/v1/statistics?groupBy=MONTH` |
| Filtr dat w statystykach | ✅ | Parametry `fromYear`, `fromMonth`, `toYear`, `toMonth` |
| Deduplikacja transakcji | ✅ | Po UUID z CSV — idempotentny import (patrz [sekcja 2](#2-założenie-uuid-transakcji)) |
| Lista importów | ✅ | `GET /api/v1/imports` |
| Lista transakcji | ✅ | `GET /api/v1/transactions?importJobId=...` |
| Dokumentacja Swagger UI | ✅ | `/swagger-ui.html` |
| Docker Compose | ✅ | Aplikacja + MongoDB + Mongo Express |
| RFC 7807 ProblemDetail | ✅ | Ujednolicony format błędów HTTP |

### Stos technologiczny

- **Java 21** · **Spring Boot 4** · **Spring Data MongoDB** · **Spring Web MVC**
- **MongoDB 8.0** · **Apache Commons CSV 1.14.1** · **Springdoc OpenAPI 2.8.8**
- **Gradle 9** (Kotlin DSL) · **JUnit 5** · **Testcontainers** · **Docker Compose**

---

## 2. Założenie: UUID transakcji

> **Plik CSV zawiera kolumnę `transactionId` z unikalnym UUID per transakcja.**

### Uzasadnienie

Wymaganie określa, że ta sama nazwa pliku może pojawiać się wielokrotnie (pliki generowane losowo przez bank). Bez identyfikatora na poziomie wiersza nie można rozstrzygnąć, czy dany rekord w nowym pliku to ta sama transakcja, która już istnieje w bazie, czy nowa transakcja o identycznych polach (np. dwie płatności w tym samym sklepie na tę samą kwotę w tej samej dacie).

**UUID jako `_id` w MongoDB** daje:
- **Idempotentność importu** — ponowne przesłanie tego samego pliku nie tworzy duplikatów
- **Bezpieczny re-import** — plik importowany częściowo (np. błąd połączenia) można bezpiecznie przesłać ponownie
- **Rozliczalność** — pole `duplicateTransactionIds` w odpowiedzi informuje, które UUID zostały pominięte w danym imporcie

```csv
transactionId,iban,transactionDate,currency,category,amount
a1b2c3d4-0001-0001-0001-000000000001,PL61109010140000071219812874,2024-01-15,PLN,GROCERIES,150.00
```

---

## 3. Szybki start z Dockerem

### Wymagania wstępne

- **Docker Desktop** (lub Docker Engine + Docker Compose plugin)
- **Java 21+** — do zbudowania JAR przed stworzeniem obrazu

### Uruchomienie w 1 kroku
Dzięki temu, że projekt jest skonfigurowany z Docker Compose, możesz uruchomić całą aplikację wraz z MongoDB i Mongo Express jednym poleceniem:

```bash
docker compose up --build
```

Po chwili dostępne:

| Usługa | Adres |
|--------|-------|
| REST API | http://localhost:8080 |
| **Swagger UI** | **http://localhost:8080/swagger-ui.html** |
| Health check | http://localhost:8080/actuator/health |
| Mongo Express (GUI bazy) | http://localhost:8081 · login: `admin` / `admin` |

### Zmienne środowiskowe

| Zmienna | Domyślnie |
|---------|-----------|
| `MONGODB_URI` | `mongodb://localhost:27017/bank_transactions` |

### Możliwości rozwoju pod produkcję
- Przerobienie Dockerfile i ładowanie obrazu do Docker Hub → uruchomienie bez konieczności budowania lokalnego
- Przerobienie docker-compose aby łądowało obraz z Docker Hub zamiast budować lokalnie

---

## 4. Format CSV

Kolumny (kolejność dowolna, nagłówki wymagane):

```csv
transactionId,iban,transactionDate,currency,category,amount
a1b2c3d4-0001-0001-0001-000000000001,PL61109010140000071219812874,2024-01-15,PLN,GROCERIES,150.00
a1b2c3d4-0001-0001-0001-000000000002,PL61109010140000071219812874,2024-01-20,PLN,TRANSPORT,-45.50
```

| Kolumna | Format | Przykład |
|---------|--------|---------|
| `transactionId` | UUID | `a1b2c3d4-0001-0001-0001-000000000001` |
| `iban` | Litery + cyfry, min. 15 znaków | `PL61109010140000071219812874` |
| `transactionDate` | `yyyy-MM-dd` | `2024-01-15` |
| `currency` | ISO 4217 (3 litery) | `PLN`, `EUR`, `USD` |
| `category` | Enum (patrz niżej) | `GROCERIES` |
| `amount` | Liczba dziesiętna | `150.00`, `-45.50` |

**Dozwolone kategorie:**  
`GROCERIES` · `TRANSPORT` · `ENTERTAINMENT` · `UTILITIES` · `DINING` · `HEALTHCARE` · `SHOPPING` · `TRAVEL` · `EDUCATION` · `SALARY` · `OTHER`

**Limity:** maks. **10 000 wierszy** · maks. **50 MB** rozmiar pliku

**Przykładowy plik:** `src/test/resources/sample-transactions.csv`

---

## 5. Endpointy API

> 📖 Interaktywna dokumentacja z możliwością wysyłania żądań: **http://localhost:8080/swagger-ui.html**

Bazowy URL: `/api/v1`  
Błędy: **RFC 7807 ProblemDetail** (pola `status`, `detail`, `errorCode`)

---

### `POST /api/v1/imports` — Prześlij plik CSV

```bash
curl -X POST http://localhost:8080/api/v1/imports \
  -F "file=@src/test/resources/sample-transactions.csv"
```

Tworzy `ImportJob` w stanie `PENDING` i **natychmiast zwraca `201 Created`**.  
Przetwarzanie CSV odbywa się asynchronicznie w tle — sprawdź status przez `GET /api/v1/imports/{id}`.

**Odpowiedź `201`:**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "PENDING",
  "fileName": "sample-transactions.csv",
  "totalRows": 0,
  "processedRows": 0,
  "validRows": 0,
  "invalidRows": 0,
  "createdAt": "2024-01-15T10:30:00",
  "errors": [],
  "duplicateTransactionIds": []
}
```

| Kod | `errorCode` | Przyczyna |
|-----|-------------|-----------|
| `400` | `bad-request` | Pusty plik |
| `413` | `file-too-large` | Plik > 50 MB |
| `415` | `unsupported-file-type` | Brak rozszerzenia `.csv` |

---

### `GET /api/v1/imports/{id}` — Status importu

```bash
curl http://localhost:8080/api/v1/imports/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

**Stany `ImportJob`:**

```
PENDING ──► PROCESSING ──► COMPLETED
                       └──► COMPLETED_WITH_ERRORS
                       └──► FAILED
```

| Status | Opis |
|--------|------|
| `PENDING` | Oczekuje na wątek roboczy |
| `PROCESSING` | Przetwarzanie w toku |
| `COMPLETED` | Wszystkie wiersze poprawnie zaimportowane |
| `COMPLETED_WITH_ERRORS` | Import zakończony — część wierszy odrzucona (szczegóły w `errors[]`) |
| `FAILED` | Krytyczny błąd — zła struktura CSV lub przekroczony limit wierszy |

**Przykład odpowiedzi po zakończeniu z błędami walidacji:**
```json
{
  "status": "COMPLETED_WITH_ERRORS",
  "totalRows": 12,
  "validRows": 10,
  "invalidRows": 2,
  "errors": [
    {
      "rowNumber": 5,
      "rawData": ",INVALID_IBAN,2024-01-10,PLN,GROCERIES,50.00",
      "errorMessage": "transactionId is required"
    }
  ],
  "duplicateTransactionIds": ["a1b2c3d4-0001-0001-0001-000000000001"]
}
```

---

### `GET /api/v1/imports` — Lista wszystkich importów

```bash
curl http://localhost:8080/api/v1/imports
```

---

### `GET /api/v1/transactions` — Lista transakcji

```bash
curl "http://localhost:8080/api/v1/transactions?importJobId=3fa85f64-5717-4562-b3fc-2c963f66afa6"
```

---

### `GET /api/v1/statistics` — Statystyki budżetowe

| Parametr | Typ | Opis |
|----------|-----|------|
| `groupBy` | `CATEGORY` \| `IBAN` \| `MONTH` | Pole grupowania (wymagany) |
| `fromYear` | int | Rok początku zakresu (opcjonalny) |
| `fromMonth` | int (1–12) | Miesiąc początku zakresu (opcjonalny) |
| `toYear` | int | Rok końca zakresu (opcjonalny) |
| `toMonth` | int (1–12) | Miesiąc końca zakresu (opcjonalny) |

```bash
# Suma wydatków per kategoria
curl "http://localhost:8080/api/v1/statistics?groupBy=CATEGORY"

# Per IBAN, tylko I kwartał 2024
curl "http://localhost:8080/api/v1/statistics?groupBy=IBAN&fromYear=2024&fromMonth=1&toYear=2024&toMonth=3"

# Trend miesięczny — wszystkie miesiące
curl "http://localhost:8080/api/v1/statistics?groupBy=MONTH"
```

**Odpowiedź:**
```json
[
  { "groupKey": "GROCERIES",  "totalAmount": 1250.00, "transactionCount": 8, "averageAmount": 156.25, "currency": "PLN" },
  { "groupKey": "TRANSPORT",  "totalAmount":  340.50, "transactionCount": 4, "averageAmount":  85.13, "currency": "PLN" },
  { "groupKey": "DINING",     "totalAmount":  890.00, "transactionCount": 6, "averageAmount": 148.33, "currency": "PLN" }
]
```

Agregacja realizowana przez **MongoDB Aggregation Pipeline** — brak operacji na danych po stronie JVM.

---

## 6. Testowanie

### Testy automatyczne

```bash
# Uruchom wszystkie testy (unit + integracyjne)
./gradlew test

# Raport HTML wyników
open build/reports/tests/test/index.html
```

> ⚠️ Testy integracyjne wymagają **Dockera** — Testcontainers uruchamia MongoDB automatycznie, bez żadnej konfiguracji.

#### Testy jednostkowe (bez Springa, bez Dockera)

| Klasa | Co weryfikuje |
|-------|--------------|
| `ImportJobTest` | State machine — przejścia stanów, liczniki, niemutowalność listy błędów |
| `TransactionValidatorTest` | Walidacja IBAN, daty, kwoty, kategorii — poprawne i błędne dane |
| `IbanTest` | Value Object IBAN — poprawne i niepoprawne formaty |
| `MoneyTest` | Value Object Money — walidacja kwoty i waluty |

#### Testy integracyjne (`ImportIntegrationTest`) — scenariusze end-to-end z MongoDB

| Test | Weryfikuje |
|------|-----------|
| `shouldReturn201WithJobIdAfterUpload` | Przyjęcie pliku, nagłówek `Location` |
| `shouldReturnPendingOrProcessingRightAfterUpload` | Status bezpośrednio po wysłaniu |
| `shouldEventuallyCompleteImport` | Asynchroniczne przetwarzanie do końca |
| `shouldReportRowLevelValidationErrors` | Liczniki i lista błędów walidacji per wiersz |
| `shouldReturn404ForUnknownJob` | 404 dla nieistniejącego `importJobId` |
| `shouldReturn404WithProblemDetailForUnknownJob` | RFC 7807 ProblemDetail przy 404 |
| `shouldReturnNonEmptyListAfterImport` | Lista importów niepusta po uploaderze |
| `shouldReturnStatisticsGroupedByCategory` | Agregacja per kategoria po imporcie |
| `shouldReturn400WithProblemDetailForInvalidGroupBy` | Walidacja parametru `groupBy` |
| `shouldReturn415WhenFileHasWrongExtension` | Odrzucenie pliku `.txt` z kodem 415 |
| `shouldFailJobWhenCsvHasWrongColumnStructure` | Job FAILED przy brakujących nagłówkach |
| `shouldHandleFileWithMultipleInvalidEntries` | COMPLETED_WITH_ERRORS, wiele błędów per wiersz |
| `shouldFailJobWhenCsvExceeds10000Rows` | Przekroczenie limitu 10 000 wierszy → FAILED |
| `shouldAcceptCsvWithExactly10000Rows` | Przypadek graniczny — dokładnie 10 000 wierszy → COMPLETED |

---

### Testowanie przez Swagger UI

1. Otwórz **http://localhost:8080/swagger-ui.html**
2. Rozwiń **Imports → `POST /api/v1/imports`** → kliknij _Try it out_
3. Kliknij _Choose File_ → wybierz `src/test/resources/sample-transactions.csv` → _Execute_
4. Skopiuj `id` z odpowiedzi `201`
5. Rozwiń **`GET /api/v1/imports/{id}`** → wklej `id` → _Execute_  
   Powtarzaj odświeżanie do momentu gdy `status` osiągnie `COMPLETED`
6. Rozwiń **Statistics → `GET /api/v1/statistics`** → ustaw `groupBy = CATEGORY` → _Execute_

---

### Testowanie przez curl (scenariusz end-to-end)

```bash
# 1. Wyślij plik i zapisz ID joba
JOB_ID=$(curl -s -X POST http://localhost:8080/api/v1/imports \
  -F "file=@src/test/resources/sample-transactions.csv" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

echo "Job ID: $JOB_ID"

# 2. Sprawdź status importu
curl -s "http://localhost:8080/api/v1/imports/$JOB_ID" | python3 -m json.tool

# 3. Pobierz statystyki po zakończeniu importu
curl -s "http://localhost:8080/api/v1/statistics?groupBy=CATEGORY" | python3 -m json.tool
curl -s "http://localhost:8080/api/v1/statistics?groupBy=MONTH" | python3 -m json.tool
```

---

## 7. Architektura

Projekt stosuje **architekturę heksagonalną (Ports & Adapters)** z podziałem na pionowe moduły funkcjonalne.

```
┌──────────────────────────────────────────────────────────┐
│                    ADAPTERS IN (HTTP)                    │
│  ImportController      /api/v1/imports                   │
│  TransactionController /api/v1/transactions              │
│  StatisticsController  /api/v1/statistics                │
└───────────────────────┬──────────────────────────────────┘
                        │  ports/in (interfejsy use case)
┌───────────────────────▼──────────────────────────────────┐
│              APPLICATION SERVICES                        │
│  UploadTransactionFileService                            │
│  ProcessImportJobService  (async via AsyncImportTriggerPort)│
│  GetImportStatusService · ListImportsService             │
│  ListTransactionsService · GetStatisticsService          │
└──────────┬───────────────────────────────────────────────┘
           │  domain logic
┌──────────▼───────────────────────────────────────────────┐
│                      DOMAIN                              │
│  ImportJob (agregat + state machine)                     │
│  Transaction · TransactionCategory · TransactionError    │
│  TransactionValidator · Iban · Money                     │
└──────────┬───────────────────────────────────────────────┘
           │  ports/out (interfejsy infrastruktury)
┌──────────▼───────────────────────────────────────────────┐
│                   ADAPTERS OUT                           │
│  MongoImportJobAdapter · MongoTransactionAdapter         │
│  MongoStatisticsAdapter · CommonsCsvReaderAdapter        │
│  ImportJobWorker  (@Async, implements AsyncImportTriggerPort)│
└──────────────────────────────────────────────────────────┘
```

**Kluczowe decyzje projektowe:**

| Temat | Decyzja | Uzasadnienie |
|-------|---------|-------------|
| Deduplikacja | `transactionId` jako `_id` MongoDB | Idempotentność — patrz [sekcja 2](#2-założenie-uuid-transakcji) |
| Async trigger | Port `AsyncImportTriggerPort` | Serwis aplikacyjny nie zależy od adaptera workera — prawidłowa heksagonalna zależność |
| Batch insert | Partie po 500 rekordów | Kompromis: wydajność vs widoczny postęp w logu |
| Agregacje | MongoDB Aggregation Pipeline | Brak agregacji w pamięci JVM; skalowalność po stronie bazy |
| Błędy HTTP | RFC 7807 `ProblemDetail` | Ujednolicony format; pole `errorCode` do programatycznej obsługi |
| Walidacja wierszy | Domenowy `TransactionValidator` | Błędny wiersz nie przerywa importu — pozostałe wiersze przetwarzane dalej |

---

## 8. Eksploracja domeny bankowej

Model domenowy opiera się na podstawowych pojęciach bankowości osobistej:

- **IBAN** — unikalny identyfikator rachunku bankowego (Value Object z walidacją formatu i minimalnej długości)
- **Transakcja** — niepodzielne zdarzenie finansowe: kwota, waluta, data, kategoria i rachunek źródłowy
- **Kategoria budżetowa** — klasyfikacja umożliwiająca analizę wzorców wydatkowych; modelowana jako enum (wartości domenowe, nie dane konfiguracyjne)
- **Import** — jeden wyciąg bankowy = jeden `ImportJob`; plik jest anonimowy (losowa nazwa), tożsamość transakcji nadana przez `transactionId` per wiersz
- **Statystyki** — agregaty odpowiadające na pytania budżetowe: _ile wydaję na jedzenie?_, _z którego konta najczęściej płacę?_, _w którym miesiącu mam największe wydatki?_

Domena jest wolna od zależności frameworkowych — walidacja, state machine i reguły biznesowe istnieją w czystej Javie i są testowalne jednostkowo bez uruchamiania Springa.

---

## 9. Propozycje rozwoju

### 1. Kolejka wiadomości zamiast `@Async` — niezawodność i skalowalność

Obecny `@Async` działa w tej samej JVM — restart aplikacji może zgubić przetwarzane joby.

**Rozwiązanie:** zastąpić `AsyncImportTriggerPort` adapterem publikującym do kolejki (Kafka / RabbitMQ / AWS SQS).  
Architektura `AsyncImportTriggerPort` już przewiduje taką wymianę bez zmian w warstwie aplikacyjnej.

```
[API Instance]  ──publish──►  [Queue: import-jobs]  ──consume──►  [Worker Instance(s)]
```

Korzyści:
- **Niezawodność** — job przeżywa restart (at-least-once delivery), możliwy retry po błędzie
- **Niezależne skalowanie** — workerów więcej niż podów API w godzinach szczytu importów
- **Backpressure** — kolejka absorbuje nagłe skoki liczby przesyłanych plików

### 2. Osobny mikroserwis worker + wiele instancji

Przy kolejce wiadomości API i worker to osobne deployable units:

- **API pod** — przyjmowanie plików, odczyt statusów, statystyki; stateless, skaluje się swobodnie
- **Worker pod** — konsumpcja z kolejki, przetwarzanie CSV, zapis do MongoDB; skaluje się per przepustowość

Dodatkowe wymagania przy wielu instancjach:
- Przechowywanie plików CSV w object storage (S3 / MinIO) zamiast w pamięci JVM — kolejka niesie tylko referencję `s3://bucket/jobId.csv`
- Distributed lock przy aktualizacji `ImportJob` (np. MongoDB `findAndModify` z warunkiem na status)

### 3. Przechowywanie pliku poza pamięcią JVM

Aktualnie `UploadTransactionFileService` kopiuje cały plik do `byte[]` przed przekazaniem do workera.  
Przy 50 MB × N równoległych uploadach może to wywierać presję na heap.

**Rozwiązanie:** streaming upload bezpośrednio do S3/MinIO → worker pobiera plik ze streamu bez bufforowania w pamięci.

### 4. Autentykacja i wielodostępność (multi-tenancy)

- OAuth2 / JWT — każdy użytkownik widzi tylko swoje importy i statystyki
- Pole `ownerId` na dokumentach `import_jobs` i `transactions` + indeks złożony `(ownerId, transactionDate)`

### 5. Rozszerzenie formatu wejściowego

Port `CsvReaderPort` izoluje format pliku — wystarczy nowy adapter implementujący ten port:
- Import z pliku XLSX (Apache POI)
- Integracja z API bankowym (PSD2 / Open Banking)
- Cykliczny automatyczny import przez scheduler

### 6. Powiadomienia o zakończeniu importu

Po przejściu `ImportJob` w `COMPLETED` / `FAILED` można opublikować event domenowy:
- Webhook (HTTP callback skonfigurowany przez klienta)
- E-mail / push notification przez nowy `ImportJobNotificationPort`

### 7. Eksport i raporty

- `GET /api/v1/statistics?format=csv` — eksport zagregowanych danych do CSV
- Generowanie raportów PDF przez adapter `StatisticsExportPort`

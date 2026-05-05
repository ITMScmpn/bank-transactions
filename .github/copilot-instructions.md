# Bank Transactions — Import & Analytics API

Narzędzie do importu wyciągów bankowych z pliku CSV oraz agregacji statystyk budżetowych (per kategoria, IBAN, miesiąc).

- Przyjmuje pliki CSV z transakcjami bankowymi (IBAN, data, waluta, kategoria, kwota, UUID transakcji)
- Przetwarza import asynchronicznie z pełnym śledzeniem statusu (PENDING → PROCESSING → COMPLETED / FAILED)
- Deduplikuje transakcje na podstawie `transactionId` (UUID z CSV) — idempotentny import
- Udostępnia zagregowane statystyki per kategoria, IBAN lub miesiąc
- Obsługuje do 10 000 wierszy CSV, pliki do 50 MB
- Architektura heksagonalna (Ports & Adapters), MongoDB, Spring Boot 4, Java 21

---

## Stos Technologiczny

- Java — 21
- Spring Boot — 4.0.6
- Spring Web (MVC) — zarządzane przez Spring Boot BOM
- Spring Data MongoDB — zarządzane przez Spring Boot BOM
- Spring Boot Actuator — zarządzane przez Spring Boot BOM
- Spring Boot Validation (jakarta.validation) — zarządzane przez Spring Boot BOM
- MongoDB — 8.0 (runtime / Docker)
- Apache Commons CSV — 1.14.1
- Springdoc OpenAPI (Swagger UI, WebMVC) — 2.8.8
- Gradle (Kotlin DSL) — 9.0.0
- JUnit 5 — zarządzane przez Spring Boot BOM
- Testcontainers (MongoDB, JUnit Jupiter) — 1.21.3
- Docker / Docker Compose — brak wersji

---

## Struktura Projektu

Wprowadzając zmiany w projekcie, zawsze postępuj zgodnie z poniższą strukturą katalogów:

src
|
+---main
|   +---java
|   |   \---com
|   |       \---banktransactions                          # root package
|   |           |   BankTransactionsApplication.java      # punkt wejścia Spring Boot
|   |           |
|   |           +---features                              # moduły funkcjonalne (Vertical Slice)
|   |           |   +---importing                         # feature: import plików CSV
|   |           |   |   +---adapter
|   |           |   |   |   +---in
|   |           |   |   |   |   \---web                   # adaptery wejściowe HTTP
|   |           |   |   |   |       |   ImportController.java         # POST/GET /api/v1/imports
|   |           |   |   |   |       |   TransactionController.java    # GET /api/v1/transactions
|   |           |   |   |   |       \---dto
|   |           |   |   |   |           |   ImportJobResponse.java    # DTO odpowiedzi importu
|   |           |   |   |   |           \---TransactionResponse.java  # DTO odpowiedzi transakcji
|   |           |   |   |   +---out
|   |           |   |   |   |   +---csv                   # adapter odczytu CSV
|   |           |   |   |   |   |   \---CommonsCsvReaderAdapter.java  # implementacja CsvReaderPort
|   |           |   |   |   |   \---mongodb               # adaptery wyjściowe MongoDB
|   |           |   |   |   |       |   ImportJobDocument.java        # dokument MongoDB dla ImportJob
|   |           |   |   |   |       |   ImportJobMongoRepository.java # Spring Data repository
|   |           |   |   |   |       |   MongoImportJobAdapter.java    # implementacja ImportJobRepositoryPort
|   |           |   |   |   |       |   MongoTransactionAdapter.java  # implementacja TransactionRepositoryPort
|   |           |   |   |   |       |   TransactionDocument.java      # dokument MongoDB dla Transaction
|   |           |   |   |   |       \---TransactionMongoRepository.java # Spring Data repository
|   |           |   |   |   \---worker                    # adapter wyzwalający async przetwarzanie
|   |           |   |   |       \---ImportJobWorker.java  # @Async – uruchamia ProcessImportJobService
|   |           |   |   +---application
|   |           |   |   |   +---port
|   |   |   |   |   +---in                    # interfejsy use case (wejście)
|   |   |   |   |   |   |   GetImportStatusUseCase.java
|   |   |   |   |   |   |   ListImportsUseCase.java
|   |   |   |   |   |   |   ListTransactionsUseCase.java
|   |   |   |   |   |   \---UploadTransactionFileUseCase.java
|   |           |   |   |   |   \---out                   # interfejsy infrastruktury (wyjście)
|   |           |   |   |   |       |   CsvReaderPort.java
|   |           |   |   |   |       |   AsyncImportTriggerPort.java
|   |           |   |   |   |       |   ImportJobRepositoryPort.java
|   |           |   |   |   |       \---TransactionRepositoryPort.java
|   |           |   |   |   \---service                   # implementacje use case
|   |           |   |   |       |   CancelImportService.java
|   |           |   |   |       |   GetImportStatusService.java
|   |           |   |   |       |   ListImportsService.java
|   |           |   |   |       |   ListTransactionsService.java
|   |           |   |   |       |   ProcessImportJobService.java      # główna logika przetwarzania CSV
|   |           |   |   |       \---UploadTransactionFileService.java
|   |           |   |   \---domain                        # logika domenowa – zero zależności frameworkowych
|   |           |   |       +---model
|   |           |   |       |   |   ImportJob.java         # agregat + state machine
|   |           |   |       |   |   ImportStatus.java      # enum stanów importu
|   |           |   |       |   |   Transaction.java       # encja domenowa
|   |           |   |       |   |   TransactionCategory.java # enum kategorii budżetowych
|   |           |   |       |   \---TransactionError.java  # record błędu walidacji wiersza
|   |           |   |       +---service
|   |           |   |       |   \---TransactionValidator.java # walidacja domenowa pól CSV
|   |           |   |       \---valueobject
|   |           |   |           |   Iban.java              # Value Object z walidacją formatu
|   |           |   |           \---Money.java             # Value Object kwota + waluta
|   |           |   \---statistics                         # feature: statystyki transakcji
|   |           |       +---adapter
|   |           |       |   +---in
|   |           |       |   |   \---web
|   |           |       |   |       |   StatisticsController.java     # GET /api/v1/statistics
|   |           |       |   |       \---dto
|   |           |       |   |           \---StatisticEntryResponse.java
|   |           |       |   \---out
|   |           |       |       \---mongodb
|   |           |       |           \---MongoStatisticsAdapter.java   # agregacja MongoDB
|   |           |       +---application
|   |           |       |   +---port
|   |           |       |   |   +---in
|   |           |       |   |   |   \---GetStatisticsUseCase.java
|   |           |       |   |   \---out
|   |           |       |   |       \---StatisticsQueryPort.java
|   |           |       |   \---service
|   |           |       |       \---GetStatisticsService.java
|   |           |       \---domain
|   |           |           \---model
|   |           |               |   GroupBy.java           # enum: CATEGORY | IBAN | MONTH
|   |           |               \---StatisticEntry.java    # wynik agregacji
|   |           \---infrastructure                         # konfiguracja i cross-cutting concerns
|   |               +---config
|   |               |   |   AsyncConfig.java               # konfiguracja ThreadPoolTaskExecutor (@Async)
|   |               |   \---MongoConfig.java               # konfiguracja MongoDB / konwersje dat
|   |               \---exception
|   |                   |   GlobalExceptionHandler.java    # @RestControllerAdvice – RFC 7807 ProblemDetail
|   |                   |   ImportJobNotCancellableException.java
|   |                   |   ImportJobNotFoundException.java
|   |                   |   InvalidCsvStructureException.java
|   |                   |   TooManyRowsException.java
|   |                   \---UnsupportedFileTypeException.java
|   \---resources
|       \---application.yml                               # konfiguracja aplikacji
\---test
    +---java
    |   \---com
    |       \---banktransactions
    |           |   ImportIntegrationTest.java             # testy end-to-end z Testcontainers
    |           \---features
    |               \---importing
    |                   \---domain
    |                       +---model
    |                       |   \---ImportJobTest.java     # unit test state machine
    |                       +---service
    |                       |   \---TransactionValidatorTest.java
    |                       \---valueobject
    |                           |   IbanTest.java
    |                           \---MoneyTest.java
    \---resources
        |   invalid-entries.csv                           # CSV z błędnymi wierszami
        |   sample-transactions.csv                       # przykładowe dane demo
        |   test-transactions.csv                         # dane testowe (mix poprawnych i błędnych)
        |   wrong-extension.txt                           # plik do testu 415
        \---wrong-structure.csv                           # plik do testu FAILED (zła struktura)

Modyfikując strukturę katalogów, zawsze aktualizuj tę sekcję.

---

## PRAKTYKI KODOWANIA

### Wytyczne dla POZIOMU WSPARCIA

#### EKSPERT WSPARCIA

- Preferuj eleganckie, łatwe w utrzymaniu rozwiązania zamiast rozwlekłego kodu. Zakładaj znajomość idiomów języka i wzorców projektowych.
- Podkreślaj potencjalne implikacje wydajnościowe i możliwości optymalizacji w proponowanym kodzie.
- Umieszczaj rozwiązania w szerszym kontekście architektonicznym i sugeruj alternatywy projektowe, gdy jest to odpowiednie.
- Skup komentarze na „dlaczego", a nie na „co" — zakładaj czytelność kodu poprzez dobrze nazwane funkcje i zmienne.
- Proaktywnie zajmuj się przypadkami brzegowymi, warunkami wyścigu i zagadnieniami bezpieczeństwa bez monitowania.
- Podczas debugowania dostarczaj ukierunkowane podejścia diagnostyczne zamiast rozwiązań na ślepo.
- Sugeruj kompleksowe strategie testowania zamiast tylko przykładowych testów, uwzględniając mockowanie, organizację testów i pokrycie.

### Wytyczne dla czystego kodu

- **Logika biznesowa** należy wyłącznie do warstwy `domain` (model, service, valueobject) — zero adnotacji frameworkowych w tej warstwie.
- **Endpointy HTTP** definiuj tylko w adapterach `adapter/in/web`; kontrolery delegują do use case, nie zawierają logiki.
- **Dostęp do bazy danych** realizuj wyłącznie przez adaptery `adapter/out/mongodb`; nigdy nie wstrzykuj repozytoriów Spring Data bezpośrednio do serwisów aplikacyjnych.
- **Obsługa błędów** domenowych: rzucaj wyjątki domenowe z warstwy `domain`/`application`, obsługuj je centralnie w `GlobalExceptionHandler` (`@RestControllerAdvice`) — zwracaj RFC 7807 `ProblemDetail`.
- **SOLID**:
  - Single Responsibility — każda klasa ma jeden powód do zmiany; serwisy aplikacyjne orkiestrują, nie implementują logiki domenowej.
  - Open/Closed — rozszerzaj przez nowe implementacje portów, nie modyfikując istniejących.
  - Liskov Substitution — implementacje portów są w pełni wymienne bez zmian w kodzie klienta.
  - Interface Segregation — porty są wąskie (jeden use case = jeden interfejs); unikaj grubych interfejsów.
  - Dependency Inversion — warstwy wewnętrzne (domain, application) nigdy nie zależą od warstw zewnętrznych; zależności wskazują do środka.
- **DRY** — nie duplikuj logiki mapowania; każdy mapper/konwerter w jednym miejscu.
- **KISS** — preferuj proste rozwiązania; nie buduj infrastruktury na zapas.
- **YAGNI** — nie implementuj funkcji, których nie wymaga aktualny use case.
- Używaj `record` do niezmiennych DTO i Value Objects.
- Nazwy metod powinny odzwierciedlać intencje domenowe (`startProcessing()`, `recordValid()`), a nie techniczne szczegóły.
- Unikaj komentarzy opisujących „co" — kod ma być samodokumentujący; komentarz tylko gdy uzasadniasz decyzję architektoniczną.
- Maksymalna głębokość zagnieżdżenia: 2–3 poziomy; wyciągaj metody pomocnicze.

### Wytyczne dla ARCHITEKTURY

#### Architektura Heksagonalna (Ports & Adapters)

- **Domena** (innermost) nie może importować klas Spring, Mongo, HTTP ani żadnej infrastruktury — czysta Java.
- **Porty wejściowe** (`application/port/in`) to interfejsy use case — jeden interfejs na jeden scenariusz biznesowy.
- **Porty wyjściowe** (`application/port/out`) to interfejsy infrastruktury wywoływane przez serwisy aplikacyjne.
- **Adaptery wejściowe** (`adapter/in`) implementują lub wywołują porty wejściowe; zawierają REST controllers, workers.
- **Adaptery wyjściowe** (`adapter/out`) implementują porty wyjściowe; zawierają repozytoria MongoDB, czytniki CSV.
- Zależności zawsze wskazują do środka: Adapter → Application → Domain. Nigdy w odwrotnym kierunku.
- Serwis aplikacyjny orkiestruje: pobiera dane przez port out, wywołuje logikę domenową, zapisuje przez port out.
- Nie mieszaj logiki persystencji z logiką domenową — dokument MongoDB (`*Document`) to wyłącznie reprezentacja danych bazy.
- Mapowanie domain ↔ document realizuj w adapterze, nie w domenie ani serwisie aplikacyjnym.
- Każdy feature (`importing`, `statistics`) jest samodzielnym modułem — unikaj zależności między featurami.
- Infrastruktura cross-cutting (konfiguracja, obsługa błędów) umieszczona jest w pakiecie `infrastructure`.

---

### Wytyczne Technologiczne

#### Wytyczne dla Java 21

- Używaj `record` dla niemutowalnych klas danych (DTO, Value Objects, błędy).
- Stosuj `sealed` classes/interfaces i pattern matching dla hierarchii typów domenowych.
- Używaj `switch` expressions zamiast `switch` statements dla `enum` i sealed types.
- Preferuj `var` gdy typ jest oczywisty z prawej strony przypisania; unikaj `var` gdy zmniejsza czytelność.
- Używaj text blocks (`"""`) dla wieloliniowych Stringów (np. JSON w testach).
- Kolekcje: preferuj `List.of()`, `Map.of()`, `Set.of()` dla niemutowalnych kolekcji; `Collections.unmodifiableList()` dla widoków na pola domenowe.
- Używaj `Optional` wyłącznie jako typ zwracany z repozytoriów/metod wyszukujących — nigdy jako pole klasy czy parametr metody.
- Preferuj `Stream` API z referencjami metodowymi zamiast anonimowych lambd.
- Używaj `instanceof` z pattern matching: `if (obj instanceof String s)`.
- Wyjątki: unchecked dla błędów domenowych i programistycznych; checked tylko gdy caller musi wymusić obsługę.
- Nie używaj `null` w API domenowym — użyj `Optional` lub wyjątku domenowego.
- Używaj `LocalDate`, `LocalDateTime` z `java.time` — nigdy `java.util.Date` ani `Calendar`.
- Stosuj `String.formatted()` zamiast `String.format()` na instancji.

#### Spring Boot 4

- Konfiguracja przez `application.yml`; grupuj właściwości logicznie; używaj `@ConfigurationProperties` dla złożonych konfiguracji.
- Każdy bean to `@Service`, `@Component`, `@Repository` lub `@RestController` — nie twórz beanów jako `@Bean` gdy Spring może je wykryć automatycznie przez component scan.
- Wstrzykiwanie wyłącznie przez konstruktor — nigdy przez pole z `@Autowired`; pozwala na łatwe testowanie bez Springa.
- Używaj `@RestControllerAdvice` + `ProblemDetail` (RFC 7807) do centralnej obsługi wyjątków — jeden handler dla całej aplikacji.
- Nie używaj `@Transactional` przy MongoDB bez replica set — projektuj operacje idempotentnie.
- Dla operacji asynchronicznych: definiuj nazwany `ThreadPoolTaskExecutor` w `@Configuration`; adnotacja `@Async("nazwa-executora")` na metodzie serwisu; nigdy `@Async` bez nazwy executora.
- Nie wstrzykuj `ApplicationContext` do klas biznesowych.
- Actuator: eksponuj tylko `health` i `info` — skonfigurowane w `management.endpoints.web.exposure.include`.
- Multipart: limituj rozmiar pliku w `application.yml` (`spring.servlet.multipart.max-file-size: 50MB`).
- Używaj `ResponseEntity<T>` w kontrolerach dla pełnej kontroli nad kodem HTTP i nagłówkami odpowiedzi.
- Nie mieszaj DTO z modelem domenowym — mapuj w warstwie adaptera.

#### Spring Data MongoDB

- Używaj `MongoTemplate.save()` dla operacji upsert (insert lub replace po `_id`) — `repository.save()` z String `@Id` zawsze wykonuje insert i rzuci `DuplicateKeyException` jeśli dokument istnieje.
- Dla złożonych zapytań z opcjonalnymi filtrami używaj `MongoTemplate` z `Query` + `Criteria` — nie buduj nazw metod `findBy...And...`.
- Agregacje (group by, sum, avg) realizuj przez `MongoTemplate.aggregate()` z `Aggregation` API — nie pobieraj danych do Java i nie agreguj w pamięci.
- Definiuj indeksy przez adnotacje `@Indexed`, `@CompoundIndex` na klasach `@Document`.
- Nie używaj `@DBRef` — preferuj denormalizację i referencje przez ID dla wydajności.
- Mapowanie dat `LocalDate`/`LocalDateTime` wymaga rejestracji konwerterów w `MongoConfig`.
- Używaj `@Id` z typem `String` (UUID) zamiast `ObjectId` dla naturalnych kluczy domenowych.
- Nie używaj `@Version` z String `@Id` — używaj `MongoTemplate.save()` dla bezpiecznego upsert.
- Nazwy kolekcji definiuj jawnie przez `@Document(collection = "nazwa")`.
- Przy `DuplicateKeyException` na transakcjach: loguj ostrzeżenie, zbieraj duplikaty w `duplicateTransactionIds` na `ImportJob`, kontynuuj import bez przerywania.

#### BIBLIOTEKI POMOCNICZE

##### Apache Commons CSV — 1.14.1

- Używaj `CSVFormat.DEFAULT.builder().setHeader(...).setSkipHeaderRecord(true).build()` dla czytelnej konfiguracji parsera.
- Zawsze zamykaj `CSVParser` w bloku `try-with-resources`.
- Waliduj nagłówki po otwarciu parsera — rzuć `InvalidCsvStructureException` jeśli wymagane kolumny nie istnieją.
- Czytaj wartości przez nazwę kolumny (`record.get("iban")`), nie przez indeks — odporne na zmianę kolejności kolumn.
- Limit wierszy egzekwuj podczas iteracji — nie czytaj całego pliku do pamięci przed sprawdzeniem.

##### Springdoc OpenAPI — 2.8.8

- Dokumentuj endpointy przez `@Operation(summary = "...")` i `@ApiResponse(responseCode = "...", description = "...")`.
- Grupuj endpointy przez `@Tag(name = "...", description = "...")` na poziomie kontrolera.
- Dla parametrów zapytania używaj `@Parameter(description = "...")`.
- Nie duplikuj dokumentacji — Springdoc czyta adnotacje Spring MVC automatycznie.
- Swagger UI dostępne pod `/swagger-ui.html`.

---

### TESTOWANIE

- **JUnit 5** — używaj `@Test`, `@BeforeEach`, `@ParameterizedTest`; nie używaj JUnit 4 (`@RunWith`).
- **AssertJ** — używaj fluent assertions (`assertThat(x).isEqualTo(y)`, `assertThat(list).isNotEmpty()`); unikaj `assertTrue`/`assertEquals` z JUnit.
- **Testcontainers** — testy integracyjne z `@Testcontainers` + `@Container @ServiceConnection`; MongoDB uruchamiane automatycznie bez ręcznej konfiguracji URI.
- Testy domenowe (unit) nie wymagają Springa — testuj `ImportJob`, `TransactionValidator`, `Iban`, `Money` jako czyste klasy Java bez `@SpringBootTest`.
- Testy integracyjne używają `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers MongoDB.
- Dla asynchronicznego kodu testuj przez polling statusu z rozsądnym timeoutem (`pollUntilDone`).
- Organizacja testów odzwierciedla strukturę main: `test/java/com/banktransactions/features/importing/domain/...`
- Nazwy testów opisują scenariusz: `shouldReturn201WithJobIdAfterUpload`, `shouldFailJobWhenCsvExceeds10000Rows`.
- Każdy test jest niezależny — nie polegaj na kolejności wykonania; izoluj stan przez Testcontainers.
- Dla testów integracyjnych używaj `RestTemplate` z wyłączonym domyślnym error handlerem, aby samodzielnie asertować kody błędów HTTP.

---

## BAZA DANYCH

### MongoDB

- Kolekcje: `import_jobs` (statusy i metadane importów), `transactions` (zaimportowane transakcje).
- Połączenie konfigurowane przez `spring.mongodb.uri` w `application.yml`; domyślnie `mongodb://localhost:27017/bank_transactions`; w Docker Compose używaj nazwy serwisu jako hosta.
- Unikalny indeks na `transactions._id` (= `transactionId` z CSV) zapewnia idempotentność importu — wielokrotny import tego samego pliku nie tworzy duplikatów.
- Nie stosuj transakcji wielodokumentowych — projektuj operacje tak, by były bezpieczne przy wielokrotnym wykonaniu.
- Dla operacji upsert używaj `MongoTemplate.save()`.
- Agregacje statystyk realizuj przez MongoDB Aggregation Pipeline — nie agreguj w pamięci Java.
- Indeksy na polach często filtrowanych: `transactions.iban`, `transactions.transactionDate`, `transactions.category`, `transactions.importJobId`.
- Mapowanie `LocalDate`/`LocalDateTime` ↔ BSON Date wymaga niestandardowych konwerterów rejestrowanych w `MongoConfig`.
- Nazwy kolekcji jawnie przez `@Document(collection = "...")`.
- Przy duplikatach `transactionId`: loguj ostrzeżenie na poziomie WARN, zapisuj UUID w `import_jobs.duplicateTransactionIds`, kontynuuj przetwarzanie kolejnych wierszy.


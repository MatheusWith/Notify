# Notify — Architecture & Conventions

## Stack Overview

| Layer       | Technology                                          |
| ----------- | --------------------------------------------------- |
| Backend     | Java 17 · Spring Boot 4.0.6 · JAR packaging         |
| Worker      | Java 17 · Spring Boot 4.0.6 · JAR packaging         |
| Frontend    | Angular 22 · TypeScript 6.0                         |
| Database    | PostgreSQL                                          |
| Gateway     | Nginx (Alpine) — único ponto de entrada             |
| Container   | Docker Compose (6 serviços)                         |
| Approach    | Domain-Driven Design (DDD) · Test-Driven Dev (TDD)  |

---

# Email System Flows

## System Roles

| Role         | Description                                                              |
|-------------|--------------------------------------------------------------------------|
| **Subscriber** | User who wants to receive emails from specific senders.                |
| **Sender**     | User authorized to create campaigns and send emails to their subscribers. |

---

# Flow 1: User Registration

**Objective:** Allow new users to create an account on the platform.

1. The user accesses the registration page.
2. Fills in the required fields:

   * Name;
   * Email;
   * Password.
3. The system validates the provided data.
4. If the data is valid:

   * The user is registered on the platform;
   * An account is created;
   * The system confirms the registration.
5. The user can log in to access their features.

**Alternative Flow** — Invalid data:

* The system displays the inconsistencies found;
* The user corrects the information and tries again.

---

# Flow 2: User Authentication

**Objective:** Allow authenticated users to access the platform.

1. The user accesses the login screen.
2. Provides:

   * Email;
   * Password.
3. The system validates the credentials.
4. If valid:

   * An authenticated session is created;
   * An access token is generated;
   * The user is redirected to the main dashboard.

**Alternative Flow** — Invalid credentials:

* The system displays an error message;
* The user stays on the login screen.

---

# Flow 3: Newsletter Subscription

**Objective:** Allow users to become subscribers of a sender.

**Example:** Users A, B, and C want to receive newsletters from user D.

1. The user accesses the sender's public profile.
2. Views information about the newsletter.
3. Clicks the "Subscribe to Newsletter" button.
4. The system registers the subscription.
5. The user joins the sender's subscriber list.
6. The subscription receives "Active" status.

**Result:** Users A, B, and C become subscribers of user D.

---

# Flow 4: Subscriber Management

**Objective:** Allow the sender to track and manage their subscriber base.

1. The sender accesses the "Subscribers" area.
2. The system retrieves all subscribers linked to their newsletter.
3. The system displays:

   * Total subscriber count;
   * Email list;
   * Subscription date;
   * Subscription status.
4. The sender can view individual subscriber information.

**Result:** The sender has full visibility of their audience.

## Backend Implementation

### API

| Method | Path | Request | Response | Description |
|--------|------|---------|----------|-------------|
| `GET` | `/api/v1/newsletter/{slug}/subscribers` | — | `Page<SubscriberResponse>` | List subscribers (paginated, authenticated, owner only) |

### Ownership & Access

- All subscriber management endpoints are authenticated via JWT
- The sender's identity is extracted from `@AuthenticationPrincipal`
- For every request, the newsletter is fetched by `slug` and `newsletter.getOwnerId()` is compared against `currentUser.id`
- Non-owners receive `403 Forbidden`
- Non-existent newsletters return `404 Not Found`

## Frontend Implementation

### Route

| Path | Component | Description |
|------|-----------|-------------|
| `/sender/newsletters/:slug` | `SubscribersPage` | View all subscribers for a newsletter |

### Subscribers Page (`subscribers-page`)

- **States**: loading (spinner), error ("Failed to load subscribers"), empty ("No subscribers yet"), data (subscriber list)
- **Display**: total subscriber count header, then a list/subscriber cards with email, name, status, and subscription date
- **Data flow**: reads `slug` from route → `GET /api/v1/newsletter/{slug}/subscribers` → renders list
- **Tests**: 8 tests covering loading, empty, error, and populated states

---

# Flow 5: Email Campaign Creation

**Objective:** Allow the sender to create, manage, and publish email campaigns for their subscribers.

**Example:** User D has subscribers A, B, and C. User D creates a campaign titled "March Newsletter" and publishes it, making it available to send.

## Status Workflow

```
DRAFT ──→ PENDING ──→ PUBLISHED ──→ SENT
  │           │
  └─── edit / delete (DRAFT & PENDING only)
```

| Status     | Description | Editable? | Deletable? | Publisheable? |
|-----------|-------------|-----------|------------|---------------|
| DRAFT     | Initial state on creation | ✅ | ✅ | ✅ (→ PENDING or → PUBLISHED) |
| PENDING   | Awaiting approval | ✅ | ✅ | ✅ (→ PUBLISHED) |
| PUBLISHED | Ready to send (Flow 6) | ❌ | ❌ | ❌ |
| SENT      | Already distributed | ❌ | ❌ | ❌ |

## Backend Implementation

### Campaign Entity (Domain)

- **Package**: `com.notify.newsletter.domain.model`
- **Fields**: `id` (UUID), `newsletterId` (UUID), `subject` (String, max 200), `content` (String, max 20000), `status` (CampaignStatus enum), `scheduledAt` (LocalDateTime, nullable), `createdAt`, `updatedAt`
- **State machine methods**:
  - `submit()` — transitions DRAFT → PENDING
  - `publish()` — transitions DRAFT|PENDING → PUBLISHED
  - `isEditable()` — true only if status is DRAFT or PENDING
  - `isDeletable()` — true only if status is DRAFT or PENDING
  - `isPublishable()` — true only if status is DRAFT or PENDING
- **Lifecycle**: `onCreate()` sets DRAFT + timestamps; `onUpdate()` refreshes updatedAt

### REST API

All endpoints are authenticated (require JWT) and verify newsletter ownership.

| Method | Path | Request | Response | Description |
|--------|------|---------|----------|-------------|
| `POST` | `/api/v1/newsletter/{slug}/campaigns` | `CreateCampaignRequest` | `CampaignResponse` (201) | Create campaign in DRAFT |
| `GET` | `/api/v1/newsletter/{slug}/campaigns` | — | `Page<CampaignResponse>` | List campaigns (paginated) |
| `GET` | `/api/v1/newsletter/{slug}/campaigns/{id}` | — | `CampaignResponse` | Get single campaign |
| `PUT` | `/api/v1/newsletter/{slug}/campaigns/{id}` | `UpdateCampaignRequest` | `CampaignResponse` | Update subject/content/schedule |
| `DELETE` | `/api/v1/newsletter/{slug}/campaigns/{id}` | — | 204 No Content | Delete (DRAFT/PENDING only) |
| `PATCH` | `/api/v1/newsletter/{slug}/campaigns/{id}/status` | `CampaignStatusRequest` | `CampaignResponse` | Transition status (to PENDING or PUBLISHED) |

### REST DTOs

```
CreateCampaignRequest   → subject (@NotBlank @Size max 200), content (@NotBlank @Size max 20000), scheduledAt (optional)
UpdateCampaignRequest   → subject (@NotBlank @Size max 200), content (@NotBlank @Size max 20000), scheduledAt (optional)
CampaignStatusRequest   → status (@NotNull, only PUBLISHED or PENDING)
CampaignResponse        → id, newsletterId, subject, content, status, scheduledAt, createdAt, updatedAt
```

### Application Layer

- **Use Case Interface**: `CampaignUseCase` defines 6 methods matching the REST endpoints
- **Application Service**: `CampaignApplicationService` implements all use cases:
  - **Ownership verification**: on every write operation, fetches the Newsletter by slug and validates `currentUser.id` matches `newsletter.getOwnerId()`
  - **Status guards**: delete and update reject if campaign is PUBLISHED or SENT via `BusinessException(409, "...")`
  - **Create**: builds Campaign from request, sets DRAFT status, persists via repository
  - **Update**: loads existing campaign, validates `isEditable()`, applies subject/content/schedule changes
  - **Status transition**: validates the requested target status is reachable from current status via the domain state machine

### Database (Flyway V7)

```sql
CREATE TABLE campaigns (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    newsletter_id UUID NOT NULL REFERENCES newsletters(id),
    subject VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'SENT')),
    scheduled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_campaigns_newsletter_id ON campaigns(newsletter_id);
CREATE INDEX idx_campaigns_status ON campaigns(status);
```

### Persistence (Infrastructure)

| Layer | Class | Role |
|-------|-------|------|
| JPA Entity | `JpaCampaignEntity` | JPA mirror with `@Entity`, `@ManyToOne` → Newsletter, `@Enumerated(STRING)` |
| Spring Data | `JpaCampaignRepository` | Extends `JpaRepository`, methods: `findByNewsletterId`, `findByNewsletterIdAndId` |
| Mapper | `CampaignDomainMapper` | Bidirectional conversion: `JpaCampaignEntity` ↔ `Campaign` (domain) |
| Adapter | `CampaignRepositoryImpl` | Implements domain `CampaignRepository`, delegates to Jpa + Mapper |

### Testing

| Test Class | Type | Tests | What it validates |
|-----------|------|-------|-------------------|
| `CampaignTest` | Unit (pure domain) | 12 | Status transitions, validation on creation, equality, state machine guards |
| `CampaignApplicationServiceTest` | Integration (`@SpringBootTest`) | 14 | Ownership verification, full CRUD flow, status transitions, error cases |
| `CampaignControllerTest` | Integration (`RANDOM_PORT`) | 12 | HTTP status codes, request validation, auth, pagination, all 6 endpoints |

## Frontend Implementation

### Routes

| Path | Component | Description |
|------|-----------|-------------|
| `/sender/newsletters/:slug/campaigns` | `CampaignListPage` | List all campaigns for a newsletter |
| `/sender/newsletters/:slug/campaigns/new` | `CampaignFormPage` | Create new campaign (create mode) |
| `/sender/newsletters/:slug/campaigns/:id/edit` | `CampaignFormPage` | Edit existing campaign (edit mode) |

### Campaign List Page (`campaign-list-page`)

- **States**: loading (spinner), empty ("No campaigns yet"), error ("Failed to load campaigns"), data (table)
- **Table columns**: Subject, Status (color-coded badge), Created, Scheduled (or "—" dash), Actions
- **Status badges**: DRAFT (gray), PENDING (blue/info), PUBLISHED (green/positive), SENT (gray)
- **Actions per status**:
  - DRAFT/PENDING: Edit button → navigates to `/campaigns/:id/edit`
  - DRAFT/PENDING: Publish button → `PATCH /status` with `{"status":"PUBLISHED"}`
  - DRAFT/PENDING: Delete button → `confirm()` dialog → `DELETE` request → reloads list
  - PUBLISHED/SENT: no action buttons rendered
- **Data flow**: On init, reads `slug` from route param → `GET /api/v1/newsletter/{slug}/campaigns` → populates table

### Campaign Form Page (`campaign-form-page`)

- **Dual mode**: `isEditMode` signal determines create vs edit
  - Create: `POST /api/v1/newsletter/{slug}/campaigns`, resets form on success
  - Edit: loads existing campaign via `GET /api/v1/newsletter/{slug}/campaigns/{id}`, then `PUT` on submit
- **Form fields**: Subject (required, max 200), Content (required, max 20000, textarea), Scheduled At (optional, `datetime-local`)
- **Validation**: Reactive Forms validators + inline error messages per field
- **States**: loading (during edit fetch), saving (button spinner), success (green notification), error (red notification)
- **Navigation**: "Back to Campaigns" link at top

---

# Flow 6: Newsletter Sending

**Objective:** Distribute a campaign to all active subscribers.

**Example:** User D has subscribers A, B, and C.

1. The sender selects a created campaign.
2. Triggers the "Send Newsletter" option.
3. The system identifies all subscribers with active status.
4. The system retrieves the corresponding email addresses.
5. The backend publishes a message to RabbitMQ.
6. **Worker** consumes the message and sends emails asynchronously.
7. Emails are sent to each subscriber.
8. The system records:

   * Send date and time;
   * Number of recipients;
   * Delivery status.

**Result:** The campaign is sent to users A, B, and C, and the send is logged for future reference.

## Worker Implementation

The worker is an independent Spring Boot 4.0.6 application that handles async email delivery via Hexagonal DDD.

### Architecture

```
┌──────────────────────────────────────┐
│  Interfaces (RabbitMQ Listener)      │  ConfirmationEmailListener — thin AMQP consumer
├──────────────────────────────────────┤
│       Application (Use Case)         │  ConsumeEmailService — orchestration + revalidation
├──────────────────────────────────────┤
│         Domain (Core)                │  EmailMessage, EmailDeliveryResult, EmailSendingService
├──────────────────────────────────────┤
│    Infrastructure (Adapters)         │  SmtpEmailSender, RabbitMQConfig, MailConfig
└──────────────────────────────────────┘
```

### Queue Flow

```
Backend Publish → newsletter.direct ──→ newsletter.subscription.confirmation.queue
                                                    │
                                                    ▼
                                          ConfirmationEmailListener
                                                    │
                                                    ▼
                                          ConsumeEmailService
                                                    │
                                                    ▼
                                          EmailSendingService (domain)
                                                    │
                                                    ▼
                                          SmtpEmailSender (SMTP via Spring Mail)
                                                    │
                                             ┌──────┴──────┐
                                             ▼              ▼
                                          Success        Failure
                                                          │
                                                     ┌────┴────┐
                                                     ▼         ▼
                                                  Retry 3x    DLQ
                                          (1s, 3s, 9s backoff)
```

### Domain Layer (`com.notify.worker.email.domain`)

| Class | Type | Fields |
|-------|------|--------|
| `EmailMessage` | Interface / Record | `messageId`, `recipientEmail`, `subject`, `body` |
| `ConfirmationEmailMessage` | Record (extends EmailMessage) | + `subscriberName`, `newsletterName`, `confirmationUrl` |
| `EmailDeliveryResult` | Record | `messageId`, `recipientEmail`, `success`, `errorMessage` |
| `EmailStatus` | Enum | `PENDING`, `SENT`, `FAILED` |
| `EmailSendingService` | Domain Service | `validateEmail()`, `createMimeMessage()` |

### Application Layer

| Port Interface | Method | Purpose |
|---------------|--------|---------|
| `ConsumeEmailUseCase` (inbound) | `process(EmailMessage)` | Entry point for processing |
| `SendEmailPort` (outbound) | `send(EmailMessage) → EmailDeliveryResult` | SMTP delivery contract |

| Application Service | Responsibility |
|--------------------|---------------|
| `ConsumeEmailService` | Revalidate (Zero Trust), delegate to domain, orchestrate sending, handle failures |

### Infrastructure

| Adapter | Connects To | Details |
|---------|------------|---------|
| `SmtpEmailSender` | SMTP (Spring `JavaMailSender`) | Async email delivery via MIME |
| `RabbitMQConfig` | RabbitMQ broker | Declares DLX/DLQ for retry |
| `MailConfig` | Spring Mail | SMTP host/port/auth config |
| `ConfirmationEmailListener` | RabbitMQ | AMQP `@RabbitListener` consumer |

### Retry & Dead Letter Queue

- **Retry**: 3 attempts, backoff 1s → 3s → 9s (geometric multiplier 3)
- **DLQ**: After 3 failures, message is routed to `newsletter.subscription.confirmation.dlq`
- **Config**: `spring.rabbitmq.listener.simple.retry.*` + `default-requeue-rejected: false`
- **Validation**: Confirmed via Testcontainers integration test (`RetryBehaviorTest`)

### Testing

| Test Class | Type | Tests | What it validates |
|-----------|------|-------|-------------------|
| `ConfirmationEmailMessageTest` | Unit (pure domain) | 10 | Construction, validation, equality |
| `EmailDeliveryResultTest` | Unit (pure domain) | 1 | Success/failure factory methods |
| `EmailSendingServiceTest` | Unit (pure domain) | 6 | Email validation, message creation |
| `ConsumeEmailServiceTest` | Integração (`@SpringBootTest`) | 4 | Use case orchestration, error handling |
| `SmtpEmailSenderTest` | Integração (Greenmail) | 3 | SMTP delivery, MIME correctness |
| `ConfirmationEmailListenerTest` | Integração (`@SpringBootTest`) | 2 | Message routing, null handling |
| `RetryBehaviorTest` | Integração (Testcontainers RabbitMQ) | 2 | DLX/DLQ configuration |

### Running Locally

```bash
cd worker
./mvnw spring-boot:run   # Starts on port 8081, connects to localhost:5672 rabbitmq
```

Requires RabbitMQ running (`docker compose up -d` from `backend/`).

---

# Backend (Java / Spring Boot)

## Directory Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/notify/
│   │   │   ├── shared/              # Cross-cutting: value objects, utils, annotations
│   │   │   ├── {bounded-context}/   # One package per Bounded Context (DDD)
│   │   │   │   ├── domain/          # Entities, Value Objects, Domain Events, Domain Services
│   │   │   │   │   ├── model/       # Aggregate roots, entities, VOs
│   │   │   │   │   ├── service/     # Domain services (stateless, side-effect-free)
│   │   │   │   │   ├── repository/  # Repository interfaces
│   │   │   │   │   └── event/       # Domain events & handlers
│   │   │   │   ├── application/     # Use-cases / application services (orchestration)
│   │   │   │   │   ├── dto/         # Input/Output DTOs
│   │   │   │   │   ├── port/        # Inbound port interfaces (use case contracts)
│   │   │   │   │   └── mapper/      # DTO <-> Domain mappers
│   │   │   │   ├── infrastructure/  # Persistence, messaging, external APIs
│   │   │   │   │   ├── repository/  # JPA entities + adapter implementations
│   │   │   │   │   │   ├── Jpa{Entity}.java    # JPA mirror entities (annotated with @Entity)
│   │   │   │   │   │   ├── Jpa{Entity}Repository.java  # Spring Data JPA repos
│   │   │   │   │   │   └── {Entity}RepositoryImpl.java  # Adapters implementing domain repos
│   │   │   │   │   ├── mapper/      # JPA Entity <-> Domain Entity mappers (optional, may reside in repository/)
│   │   │   │   │   └── config/      # Infra config (DB, messaging)
│   │   │   │   └── interfaces/      # Controllers, REST, WebSocket endpoints
│   │   │   │       └── rest/        # REST controllers
│   │   │   └── NotifyApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/        # Flyway / Liquibase migrations
│   └── test/
│       └── java/com/notify/
│           ├── shared/              # Shared test utilities, fixtures
│           └── {bounded-context}/
│               ├── domain/          # Unit tests (pure domain, no Spring context)
│               ├── application/     # Unit + integration tests
│               └── interfaces/      # Controller integration tests (MockMvc / WebTestClient)
├── pom.xml                           # Maven (preferred) or build.gradle
└── docker-compose.yml                # Local PostgreSQL instance

## Deploy Directory

```
deploy/
├── docker-compose.yml                # Full stack orchestration (6 services)
├── .env                              # COMPOSE_PROJECT_NAME=notify
├── nginx/
│   ├── nginx.conf                    # Main config: gzip, timeouts, default_server 444
│   └── conf.d/
│       ├── gateway.conf              # API proxy, static cache, SPA fallback
│       └── security.conf             # Rate limiting zones, server_tokens off
├── backend/
│   └── Dockerfile                    # Multi-stage: jdk-alpine + maven → jre-alpine
├── frontend/
│   └── Dockerfile                    # Multi-stage: node:22-alpine → nginx:alpine
├── worker/
│   └── Dockerfile                    # Multi-stage: jdk-alpine + maven → jre-alpine (no HTTP port)
└── smoke-test.sh                     # Reusable CI test script (12 tests)
```
```

## Architecture: Layered DDD

```
┌──────────────────────────────────────┐
│        Interfaces (REST/WS)          │  Spring Controllers — thin, depend on port interfaces
│            [Inbound Ports]           │  AuthUseCase, UserUseCase, NewsletterUseCase, CampaignUseCase
├──────────────────────────────────────┤
│       Application (Use Cases)        │  Implements port interfaces, orchestrates domain
├──────────────────────────────────────┤
│          Domain (Core)               │  Pure entities — no JPA — annotated with Lombok + Jakarta Validation
├──────────────────────────────────────┤
│      Infrastructure (Adapters)       │  JPA entities + repositories + mappers (implement domain interfaces)
└──────────────────────────────────────┘
```

### Principles (Hexagonal Architecture / Ports & Adapters)

- **Domain is pure**: Domain entities do NOT use JPA annotations. `@Entity`, `@Table`, `@Id`, `@Column`, `@ManyToMany` stay exclusively in JPA mirror entities under `infrastructure/repository/`.
- **Value Objects** keep `@Embeddable` (medium purity concession) to simplify mapping.
- **Repository interfaces** in the domain are PURE interfaces (do not extend `JpaRepository`). They only define `findById`, `save`, `findByEmail`, etc. The concrete implementation lives in `infrastructure/repository/`.
- **Inbound ports** (`application/port/in/`) are interfaces defining the use cases. Application services implement them. Controllers depend on the interfaces, not concrete implementations.
- **Persistence adapters** (`{Entity}RepositoryImpl`) implement the domain interfaces, delegate to Spring Data `JpaRepository`, and convert between JPA entities and domain entities via mappers.
- **Domain layer** can use Spring's `@Service` on domain services when needed — but maintains zero dependencies on external frameworks (messaging, API clients, etc.).
- **Application Services** use `@Service`, `@Transactional` and coordinate domain objects, manage transactions, and publish events via `ApplicationEventPublisher`. They are the system's use case boundary.
- **Controllers** depend on interfaces (`AuthUseCase`, `UserUseCase`, `NewsletterUseCase`) — never on concrete implementations. They parse input, validate (Jakarta), call the use case, return DTO. No business logic.
- **Dependency injection** is constructor-based (with Lombok's `@RequiredArgsConstructor`). No field injection.

### DDD Tactical Patterns

- **Aggregates**: Cluster of entities/values treated as a unit. One aggregate = one transaction boundary. Reference other aggregates only by ID.
- **Value Objects**: Immutable, self-validating on construction. Marked with `@Embeddable`. No setters. Replaceable, not changeable.
- **Domain Events**: Record what happened (`UserRegistered`, `NotificationSent`). Published by application services after domain operations complete.
- **Repositories**: Collection-like interface per aggregate root. `findById`, `save`, `delete` — never expose raw data access.

### Annotations Reference

| Annotation           | Where to Use                         | Purpose                                   |
| ------------------- | -------------------------------------- | -------------------------------------------- |
| **Lombok**          |                                        |                                              |
| `@Data`             | Entities, VOs, DTOs                    | `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` + `@RequiredArgsConstructor` |
| `@Builder`          | Entities, VOs, DTOs                    | Fluent constructor (Builder pattern)         |
| `@NoArgsConstructor`| JPA Entities                           | Empty constructor (JPA required)             |
| `@AllArgsConstructor` | DTOs, VOs                            | Constructor with all parameters              |
| `@RequiredArgsConstructor` | Services, Controllers          | Constructor with `final` fields for DI       |
| `@Slf4j`            | Services, Controllers                  | Automatic SLF4J logger                       |
| **JPA (Jakarta Persistence)** | *Only in JPA mirror entities (infrastructure/repository/)* | |
| `@Entity`           | JPA mirror entities                    | ORM mapping — database entity                |
| `@Table`            | JPA mirror entities                    | Customizes table name                        |
| `@Id`               | JPA mirror entities                    | Primary key                                  |
| `@GeneratedValue`   | JPA mirror entities                    | ID generation strategy                       |
| `@Column`           | JPA mirror entities / VOs             | Customizes column name/constraints           |
| `@Embeddable`       | Value Objects                          | VO embedded in entity table                  |
| `@Embedded`         | JPA mirror entities                    | Indicates an embedded VO                     |
| `@Enumerated`       | JPA mirror entities                    | Enum mapping (`ORDINAL` or `STRING`)         |
| `@ManyToOne` / `@OneToMany` | JPA mirror entities            | Relationship cardinality                     |
| `@JoinColumn`       | JPA mirror entities                    | Customizes FK                                |
| **Spring**          |                                        |                                              |
| `@Service`          | Application / Domain Services          | Service bean (business layer)                |
| `@Repository`       | Repository interfaces                  | Repository bean + exception translation      |
| `@RestController`   | REST Controllers                       | Controller bean + `@ResponseBody`            |
| `@RequestMapping`   | Controller class                       | Route prefix (e.g., `/api/v1/users`)         |
| `@GetMapping` / `@PostMapping` / etc | Controller methods | HTTP endpoint mapping                   |
| `@Transactional`    | Application Services / Repositories     | Transaction management                       |
| `@RequiredArgsConstructor` | Services, Controllers (Lombok)   | Automatic constructor DI                     |
| `@ControllerAdvice` | Global error handler                   | Centralized exception handling               |
| `@ExceptionHandler` | Methods in `@ControllerAdvice`         | Maps exception → HTTP response               |
| `@Configuration`    | Configuration classes                  | Defines Spring beans / config                |
| **Jakarta Validation** |                                       |                                              |
| `@NotBlank` / `@NotNull` | DTO / Entity fields                | Input validation                             |
| `@Email`            | Email fields                           | Validates email format                       |
| `@Size`             | String/collection fields               | Validates min/max length                     |
| `@Valid`            | Controller parameter                   | Activates DTO input validation               |

### Naming Conventions

- Classes: `PascalCase` — `UserRegistrationService`, `NotificationRepository`
- Methods: `camelCase` — `findById`, `registerUser`
- Constants: `UPPER_SNAKE_CASE` — `MAX_RETRY_COUNT`
- Packages: `lowercase` with dot notation — `com.notify.user.domain.model`
- DTOs: `{UseCase}Request` / `{UseCase}Response` — `RegisterUserRequest`, `UserResponse`
- Mappers: `{Source}To{Target}Mapper` — `UserToUserResponseMapper`
- Use Case Interfaces (ports): `{UseCase}` — `AuthUseCase`, `UserUseCase`, `NewsletterUseCase`
- Repository Infrastructure: `Jpa{Entity}Repository` (Spring Data) + `{Entity}RepositoryImpl` (adapter)
- JPA entities prefix with `Jpa` — `JpaUserEntity`, `JpaRoleEntity`

### Testing (TDD)

**All code is written test-first.** This is not negotiable.

| Layer         | Test Type         | Annotations / Tools                                       |
| ------------- | ----------------- | --------------------------------------------------------- |
| Domain        | Unit              | JUnit 5 · AssertJ · Mockito                               |
| Application   | Unit + Integration| JUnit 5 · `@SpringBootTest` (only sliced if possible)     |
| Infrastructure| Integration       | `@DataJpaTest` · `@Testcontainers`                        |
| Interfaces    | Integration       | `@SpringBootTest(RANDOM_PORT)` · RestTemplate             |

> **Note on Spring Boot 4.0**: Unlike Spring Boot 3.x, 4.0 removed `@WebMvcTest`, `@AutoConfigureMockMvc`, and `TestRestTemplate`. Controller tests use `@SpringBootTest` with `webEnvironment = RANDOM_PORT` and `RestTemplate` or `WebTestClient` (add `spring-boot-starter-webflux` as a test dependency).

- **Domain tests** do not load the Spring context when testing pure business rules. JUnit + AssertJ suffice. Tests involving persistence or integration can use Spring slices with `@DataJpaTest`.
- **Persistence tests** use Testcontainers with a real PostgreSQL instance — never H2 in-memory.
- **Test methods**: `given{State}_when{Action}_then{ExpectedResult}` — e.g. `givenValidUser_whenRegister_thenReturnsCreated`.
- One test class per production class. Same package structure under `test/`.

### Database

- PostgreSQL via **Docker Compose** for local dev.
- **Migrations**: Flyway (preferred) or Liquibase. All DDL changes are versioned scripts in `resources/db/migration/`.
- Schema naming: `snake_case` — `user_accounts`, `notification_logs`.
- Tables are plural; join tables use both table names: `user_notification_channel`.
- Primary keys: `BIGSERIAL` / `UUID` (prefer UUID for aggregates exposed via API).
- Foreign keys: `{referenced_table}_id` in `snake_case`.

### Data Model (Current Schema)

| Entity | Table | Key | Relationships |
|--------|-------|-----|---------------|
| User | `users` | UUID `id` | — |
| UserRole | `user_roles` | UUID `id` | `user_id` → users |
| Newsletter | `newsletters` | UUID `id` | `owner_id` → users |
| Subscription | `subscriptions` | UUID `id` | `newsletter_id` → newsletters, `subscriber_id` → users |
| Campaign | `campaigns` | UUID `id` | `newsletter_id` → newsletters |

**Flyway migrations applied**: V1 (identity), V2 (token version), V3 (subscriptions), V4 (newsletters), V5 (seed data), V6 (subscriber id), V7 (campaigns)

### API Design

- RESTful over JSON.
- Base URL: `/api/v1/{resource}`.
- Standard HTTP methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.
- Response envelope (consistent): `{ data, meta, errors }`.
- Pagination: `GET /api/v1/{resource}?page=0&size=20&sort=field,asc`.
- Error responses follow RFC 7807 (Problem Details).
- Validation: Jakarta Bean Validation on request DTOs.

### Dependency Injection

- Constructor injection exclusively. Use Lombok's `@RequiredArgsConstructor` to auto-generate the constructor with parameters for all `final` fields.
- No field injection (`@Autowired` on fields). Never.
- One class per injected dependency set — no constructor with 7+ parameters (smell → split).

---

# Frontend (Angular 22 / TypeScript 6.0)

## Directory Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/                 # Singleton services, guards, interceptors, core module
│   │   │   ├── services/         # API client, auth, storage
│   │   │   ├── interceptors/     # HTTP interceptors (auth, error, logging)
│   │   │   └── guards/           # Route guards
│   │   ├── shared/               # Reusable: components, directives, pipes, models
│   │   │   ├── components/       # Generic UI components (buttons, modals, tables)
│   │   │   ├── directives/
│   │   │   ├── pipes/
│   │   │   └── models/           # TypeScript interfaces / types (shared across features)
│   │   ├── features/             # Feature modules, one per business domain
│   │   │   └── {feature}/
│   │   │       ├── pages/        # Routable page components
│   │   │       ├── components/   # Feature-specific presentational components
│   │   │       ├── services/     # Feature-specific services (API calls, state)
│   │   │       ├── models/       # Feature-specific interfaces / enums
│   │   │       └── {feature}.routes.ts
│   │   ├── layouts/              # Layout components (sidebar, header, shell)
│   │   └── app.routes.ts         # Root route definitions
│   ├── assets/
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.prod.ts
│   ├── styles/                   # Global SCSS variables, mixins, resets
│   ├── index.html
│   ├── main.ts
│   └── styles.scss
├── angular.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.spec.json
└── package.json
```

## Architecture: Standard Angular Conventions

- **Standalone components** (NgModule-free — `standalone: true` is implicit in Angular 22). `{feature}.routes.ts` files use `loadComponent` in the router config.
- **Feature modules** are logical groupings in folders, not NgModules. Each feature is a folder with `routes.ts`, `pages/`, `components/`, `services/`, `models/`.
- **Core vs Shared vs Feature**: Core is for app-wide singleton services. Shared is for reusable UI. Feature is for business-domain views.
- **OnPush change detection** on all components. No `ChangeDetectorRef.markForCheck()` gymnastics — use Signals.
- **Angular Signals** for state management. No NgRx unless complexity demands it.
- **HttpClient** + typed services for API communication.
- **Responsive design**: All pages must adapt to the most commonly used layouts (desktop, tablet, mobile). Use CSS `light-dark()`, CSS Grid/Flexbox, relative units, and media queries — no fixed-width layouts.

## Naming Conventions

| Artifact       | Pattern                    | Example                          |
| -------------- | -------------------------- | -------------------------------- |
| Component      | `{name}.ts` (Angular 22 — `.component` suffix is optional) | `user-list.ts`                  |
| Service        | `{name}.service.ts`        | `user.service.ts`                |
| Guard          | `{name}.guard.ts`          | `auth.guard.ts`                  |
| Interceptor    | `{name}.interceptor.ts`    | `auth.interceptor.ts`            |
| Pipe           | `{name}.pipe.ts`           | `format-date.pipe.ts`            |
| Directive      | `{name}.directive.ts`      | `tooltip.directive.ts`           |
| Interface/Type | `{name}.types.ts`          | `user.types.ts`                  |
| Route file     | `{feature}.routes.ts`      | `user.routes.ts`                 |
| CSS class      | `kebab-case`               | `.user-card`, `.notification-badge` |
| TS selectors   | `kebab-case` prefix        | `selector: 'app-user-list'`      |
| Folder names   | `kebab-case` (Angular CLI default) | `user-profile/`, `notification-preferences/` |

## Testing (TDD)

- **Vitest** (Angular 22 default — replaces Karma/Jasmine).
- One `.spec.ts` file per component/service/guard/pipe.
- Tests written before implementation code.
- Component tests: `TestBed.configureTestingModule` with `imports: [Component]`.
- Service tests: `provideHttpClient()` + `provideHttpClientTesting()` (replaces `HttpClientTestingModule`).
- Coverage target: ≥ 90% lines, ≥ 80% branches.

## State Management

- **Angular Signals** as the primary reactive primitive.
- **Signal stores** (`@angular/core` stability pending) or lightweight service with `signal()` + `computed()`.
- If cross-feature state grows beyond what signals handle cleanly → adopt **NgRx Signal Store** (not the traditional NgRx Store with reducers/effects).
- Server state: loading/error/data signals in feature services. No cache layer unless needed; HTTP interceptor can handle global loading/error state.

## API Communication

- A single `ApiService` or generated OpenAPI client in `core/services/`.
- Feature services wrap API calls with typed responses and error handling.
- Interceptors handle: auth token injection, 401 redirect, global error toast.

---

# Development Workflow

### Branch Strategy

```
main            — Production-ready. Protected. Only merge via PR.
├── dev         — Integration branch. Feature branches merge here.
└── feat/*      — Feature branches. One per task/story.
```

- Feature branch naming: `feat/{short-description}` — `feat/user-registration`, `feat/notification-push`.
- Bugfix branch naming: `fix/{issue-description}` — `fix/login-redirect-loop`.
- No direct commits to `main` or `dev`. All changes flow through PRs.

### PR Requirements

- At least 1 reviewer approves.
- All tests pass (CI).
- No quality violations: Spotless formatting, PMD/SpotBugs warnings, and ESLint/Prettier must all pass.
- Coverage threshold: ≥ 80% lines, ≥ 70% branches.
- PR title describes the change, not the ticket number.
- PR body includes: **What** changed, **Why**, **How to test**.
- **Every commit with testable code MUST include corresponding tests.** Todo código testável adicionado ou modificado em um commit deve vir acompanhado de seus testes (unitários, integração ou smoke conforme o contexto). Commits que introduzem lógica testável sem testes serão rejeitados na revisão.

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

O que foi alterado:
[Descrição concisa das modificações exatas no código]

Por que foi alterado:
[Contexto de negócios/técnico por trás da alteração]

Onde interfere:
[Impacto colateral ou áreas/módulos afetados pela mudança]
```

Todo commit **obrigatoriamente** documenta três pilares no corpo da mensagem:

1. **O que foi alterado** — Descreva de forma concisa as modificações exatas feitas no código (ex: novas funções, refatorações, remoções).
2. **Por que foi alterado** — Explique o motivo ou o contexto de negócios/técnico por trás da alteração (ex: correção do bug #42, otimização de performance, nova feature de checkout).
3. **Onde interfere** — Identifique o impacto colateral ou as áreas/módulos do sistema que são afetados por essa mudança (ex: afeta a renderização do front-end, altera o tempo de resposta da API X, sem impactos externos).

| Type       | When to use                   |
| ---------- | ----------------------------- |
| `feat`     | New feature                   |
| `fix`      | Bug fix                       |
| `test`     | Adding/updating tests         |
| `refactor` | Refactoring (no behavior change) |
| `chore`    | Build, CI, dependencies       |
| `docs`     | Documentation                 |
| `style`    | Formatting (no logic change)  |

Examples:
```
feat(user): add register endpoint with email verification

O que foi alterado:
Adicionado endpoint POST /api/v1/auth/register com validação de email e hash de senha.

Por que foi alterado:
Necessidade de permitir que novos usuários criem conta na plataforma (Flow 1).

Onde interfere:
Sem impactos externos — endpoints existentes não foram modificados.
---

fix(notification): handle null recipient on send

O que foi alterado:
Adicionada guarda null no método send() para verificar destinatário antes de enviar.

Por que foi alterado:
Correção do bug #42 — NullPointerException ao enviar notificação sem destinatário.

Onde interfere:
Afeta apenas o módulo de email, nenhuma API ou banco de dados alterado.
---

### Pre-commit Automation (Husky + lint-staged)

All commits must pass format + lint checks before they are allowed. This is enforced locally via **Husky** and **lint-staged**, and again in CI.

**Pre-commit hook runs (on staged files only):**
1. `lint-staged` triggers:
   - `backend/*.java` → `./mvnw spotless:check -q`
   - `frontend/*.ts` → `eslint --fix + prettier --write`
   - `frontend/*.html` → `prettier --write`
   - `frontend/*.scss` → `prettier --write`
   - `frontend/*.json` → `prettier --write`
2. If ANY check fails → commit is **ABORTED**.
3. Fix with `npm run quality:fix` then re-stage and commit.

**Setup (one-time — already configured):**
```bash
cd frontend && npm install --save-dev husky lint-staged
cd .. && npx --prefix frontend husky init
```

### TDD Cycle (Red-Green-Refactor)

1. **Red** — Write a failing test that describes the desired behavior
2. **Green** — Write the minimal production code to make it pass
3. **Refactor** — Improve code quality while keeping tests green

Never write production code without a preceding failing test. Never delete or `@Disabled` a failing test to make CI pass — fix the code.
Never remove existing tests from a feature unless the feature itself has actually changed to make them invalid. Tests are not removed because they "seem unnecessary" or "don't test anything useful" — they exist for a reason and must stay unless the behavior they cover was intentionally modified.

---

## CI/CD Pipeline (GitHub Actions)

### Trigger
- Every Pull Request to `dev` or `main`
- Every push/merge to `main`

### Pipeline Stages

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ 1. Format   │ ──→ │ 2. Lint     │ ──→ │ 3. Test     │ ──→ │ 4. Build    │
│   Check     │     │   Check     │     │   + Cover.  │     │             │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
       │                  │                    │                    │
       ▼                  ▼                    ▼                    ▼
   FAIL → Block       FAIL → Block        FAIL → Block         FAIL → Block
       │                  │                    │                    │
       └──────────────────┴────────────────────┴────────────────────┘
                                  │
                                  ▼
                            ✅ ALL PASS
                                  │
                                  ▼
                     ┌──────────────────────┐
                     │ 5. docker-smoke      │
                     │   Build + Smoke Test │
                     └──────────────────────┘
                                  │
                                  ▼
                         PR Mergable / Deploy
```

**Stage Details:**

| Stage | Backend | Frontend | Docker |
|-------|---------|----------|--------|
| **Format** | `./mvnw spotless:check` | `npm run format:check` | — |
| **Lint** | `./mvnw pmd:check && ./mvnw spotbugs:check` | `npm run lint` | — |
| **Test** | `./mvnw clean test` (coverage 80%/70%) | `npm test -- --coverage` (coverage 80%/70%) | — |
| **Build** | `./mvnw -DskipTests package` | `npm run build` | — |
| **Docker** | — | — | `docker compose build + smoke-test.sh` |

**Artifacts:**
- Backend: `target/*.jar`
- Frontend: `dist/`
- Docker: images built via `docker compose` (gateway, frontend, backend)

**Notification:**
- PR comment with test summary and coverage report
- GitHub Check status for each stage

**Failure handling:**
- Any stage FAIL → PR cannot merge. Status check required.
- Coverage below threshold → PR annotated with uncovered lines.

---

# Running the Application

## Desenvolvimento

### Backend (Spring Boot)

#### Prerequisites

- Java 17+
- Docker (for PostgreSQL)

#### Database

```bash
cd backend
docker compose up -d
```

Starts PostgreSQL 16 on port `5432`, database `notify`, user/password `notify`.

#### Run

```bash
cd backend
./mvnw spring-boot:run
```

The application starts at `http://localhost:8080`. The `dev` profile is activated by default, reading `application-dev.yml`. Flyway runs migrations automatically on startup.

## Produção (Docker Full Stack)

```bash
# Build e start todos os serviços
docker compose -f deploy/docker-compose.yml up --build -d

# Ver status dos containers
docker compose -f deploy/docker-compose.yml ps

# Logs em tempo real
docker compose -f deploy/docker-compose.yml logs -f

# Parar e limpar volumes (dados são perdidos)
docker compose -f deploy/docker-compose.yml down -v
```

Acessar: `http://localhost:80`

Credenciais padrão: `admin@notify.com` / `Admin@123` (ADMIN) ou `test@notify.com` / `Test@123` (USER)

### Tests

```bash
cd backend
./mvnw clean test
```

Uses **Testcontainers** — spins up a temporary PostgreSQL via Docker automatically. No need for local `docker compose` to run tests.

Currently **149 tests** across all layers:
- Domain: CampaignTest, domain entities, ConfirmationEmailMessageTest, EmailDeliveryResultTest, EmailSendingServiceTest
- Application: CampaignApplicationServiceTest, UserApplicationServiceTest, AuthApplicationServiceTest, ConsumeEmailServiceTest
- Controllers: CampaignControllerTest, AuthControllerTest, UserControllerTest, HealthControllerTest
- Worker Integration: SmtpEmailSenderTest, ConfirmationEmailListenerTest, RetryBehaviorTest

### Quality Checks

```bash
cd backend
./mvnw spotless:check        # Check formatting via Spotless
./mvnw spotless:apply        # Auto-fix formatting
./mvnw pmd:check             # Static analysis via PMD
./mvnw pmd:pmd               # Generate PMD report
./mvnw spotbugs:check        # Bug detection via SpotBugs
./mvnw spotbugs:spotbugs     # Generate SpotBugs report
```

### Seed Data

The migration `V1__create_identity_tables.sql` automatically creates:

| Email | Password | Role |
|---|---|---|
| `admin@notify.com` | `Admin@123` | ADMIN |
| `test@notify.com` | `Test@123` | USER |

---

## Frontend (Angular 22)

### Prerequisites

- Node.js 22+
- npm 11+

### Run

```bash
cd frontend
npm install    # first time only
npm start      # ng serve
```

Starts at `http://localhost:4200`. By default it redirects to `/auth/login`.

### Tests

```bash
cd frontend
npm test
```

Uses **Vitest** with jsdom — no browser needed. Unit tests for components and services.

Currently **57 tests** across all components:
- AppComponent (2), ApiService (2), LoginPage (13), RegisterPage (15)
- NewsletterProfilePage (1)
- SubscribersPage (8)
- CampaignListPage (8), CampaignFormPage (8)

### Quality Checks

```bash
cd frontend
npm run format:check        # Prettier format check
npm run lint                 # ESLint static analysis
npm run quality              # Both format + lint
```

---

# Deploy (Nginx Gateway + Docker)

## Architecture

```
User → :80 → NGINX Gateway → /api/v1/* → Backend :8080
                             → *.js|.css|.ico → Static files (1y cache)
                             → /* → index.html (SPA fallback)
                             → other Host → 444 (connection drop)
```

## Stack (6 containers)

| Service   | Image                              | Ports Exposed | Dependencies        |
| --------- | ---------------------------------- | ------------- | ------------------- |
| `nginx`   | `nginx:alpine` (via frontend build)| `:80` → `:80` | backend, frontend   |
| `backend` | `eclipse-temurin:17-jre-alpine`    | —             | postgres, rabbitmq  |
| `frontend`| `nginx:alpine` (static assets)     | —             | —                   |
| `postgres`| `postgres:16`                      | —             | —                   |
| `rabbitmq`| `rabbitmq:4-management-alpine`     | —             | —                   |
| `worker`  | `eclipse-temurin:17-jre-alpine`    | —             | rabbitmq            |

Apenas a porta 80 é exposta ao host. Backend, banco e fila são acessíveis apenas dentro da rede Docker.

## Nginx Configuration

### `nginx.conf` — Main configuration

- Gzip compression for text assets
- `client_max_body_size 1M`, timeouts de 5s
- `default_server` com `return 444` — bloqueia qualquer Host não configurado
- `resolver 127.0.0.11` para resolução DNS do Docker em runtime (upstream `backend:8080`)

```nginx
server {
    listen 80 default_server;
    server_name _;
    return 444;
}
```

### `conf.d/gateway.conf` — API proxy + static assets + SPA

| Location              | Behavior                                   |
| --------------------- | ------------------------------------------ |
| `/api/v1/`            | Proxy para `upstream backend` (backend:8080) com headers de forwarding |
| `\.(js\|css\|ico\|png\|svg\|woff2?)$` | Serve do disco, `Cache-Control: public, immutable, max-age=31536000` |
| `/`                   | `try_files $uri $uri/ /index.html` (fallback SPA) |

Security headers enviados em todas as respostas:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy` restritivo (camera, microphone, geolocation bloqueados)

### `conf.d/security.conf` — Rate limiting

```nginx
limit_req_zone $binary_remote_addr zone=api_per_ip:10m rate=10r/s;
limit_conn_zone $binary_remote_addr zone=per_ip:10m;
server_tokens off;
```

- 10 requisições/segundo por IP na API (com burst de 20)
- 10 conexões simultâneas por IP
- `server_tokens off` oculta versão do Nginx

## Dockerfiles

### `backend/Dockerfile` — Multi-stage

| Stage     | Base Image                          | Ação                    |
| --------- | ----------------------------------- | ----------------------- |
| `build`   | `eclipse-temurin:17-jdk-alpine`     | `apk add maven` → `mvn package` |
| `runtime` | `eclipse-temurin:17-jre-alpine`     | `COPY` do JAR → `java -jar` |

- `mvn dependency:go-offline` em camada separada para cache de dependências
- `-Dspotless.check.skip=true` no build Docker (qualidade executada no CI/pre-commit, não no container)
- Porta 8080 exposta apenas internamente

### `frontend/Dockerfile` — Multi-stage

| Stage     | Base Image                          | Ação                         |
| --------- | ----------------------------------- | ---------------------------- |
| `build`   | `node:22-alpine`                    | `npm ci` → `ng build --production` |
| `runtime` | `nginx:alpine`                      | `COPY` do `dist/` → `/usr/share/nginx/html` |

- `npm ci --ignore-scripts` para evitar o hook do Husky no container
- Build output: `dist/frontend/browser/`
- A imagem `notify-nginx` (gateway) e `notify-frontend` usam o mesmo Dockerfile com configs diferentes

## Running the Full Stack

```bash
# Build e start todos os serviços
docker compose -f deploy/docker-compose.yml up --build -d

# Ver status
docker compose -f deploy/docker-compose.yml ps

# Logs
docker compose -f deploy/docker-compose.yml logs -f

# Parar e limpar volumes
docker compose -f deploy/docker-compose.yml down -v
```

Acessar: `http://localhost:80`

## Smoke Tests

O script `deploy/smoke-test.sh` executa 12 testes de fumaça via gateway Nginx:

| # | Teste                          | O que verifica                             |
|---|--------------------------------|--------------------------------------------|
| F1 | Default server blocking        | `return 444` para Host desconhecido        |
| F2 | SPA serving                    | `GET /` → 200, contém `<title>`            |
| F3 | SPA routing                    | Deep link → 200 (fallback index.html)      |
| F4 | API health                     | `GET /api/v1/health` → 200                 |
| F5 | Authentication                 | Login válido 200, inválido 401             |
| F6 | Authorization                  | `/users/me` autenticado 200, anônimo 401   |
| F7 | Newsletter                     | Slug existente 200, inexistente 404        |
| F8 | Subscribers                    | Owner 200, sem auth 401                    |
| F9 | Campaign CRUD                  | Create 201, list 200, update 200, publish 200, delete 409 |
| F10| Static assets                  | Cache-Control público e imutável (1 ano)   |
| F11| Security headers               | X-Content-Type-Options, X-Frame-Options    |
| F12| Rate limiting                  | 503 após ~22 requisições rápidas           |

```bash
# Executar smoke tests localmente
bash deploy/smoke-test.sh http://localhost:80
```

---

# Frequent Errors & How to Handle Them

## Backend

### "org.testcontainers.containers.ContainerLaunchException: Could not create/start container"
- **Cause**: Docker is not installed, not running, or the user doesn't have permission to access the Docker socket.
- **Fix**: Ensure Docker is running (`docker ps`). On Linux, verify the user is in the `docker` group. Restart Docker daemon if needed: `sudo systemctl restart docker`.

### "org.flywaydb.core.api.FlywayException: Found non-empty schema(s) 'public' but no schema history table"
- **Cause**: A previous Flyway migration failed mid-way, or DDL was applied manually outside Flyway, leaving the schema in an inconsistent state.
- **Fix**: Run `./mvnw flyway:repair` to fix the schema history table. If the schema is irreparably dirty, drop and recreate: `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` — then re-run `./mvnw clean test` (Testcontainers auto-creates a fresh DB).

### "org.flywaydb.core.api.FlywayException: Validate failed: Migration V7 description mismatch"
- **Cause**: The migration file was edited after it was already applied to the database.
- **Fix**: Rename the migration file with a new version (e.g., V8) or run `flyway:repair` if only the checksum is wrong. **Never edit an applied migration** — create a new one.

### "jakarta.validation.ConstraintViolationException" or 400 on valid-looking input
- **Cause**: Jakarta Bean Validation is rejecting the request. The DTO has annotations (`@NotBlank`, `@Size`, `@Email`) that the input doesn't satisfy. Most commonly: whitespace-only strings, strings exceeding max length, or missing fields.
- **Diagnose**: Check the response body — it contains a list of violations with field names and messages. Example:
  ```json
  { "violations": [{ "field": "subject", "message": "must not be blank" }] }
  ```
- **Fix**: Ensure the client sends all required fields with valid values.

### "com.notify.shared.application.BusinessException: Campaign cannot be edited in its current status"
- **Cause**: An attempt to `PUT` (update) or `DELETE` a campaign that is in `PUBLISHED` or `SENT` status.
- **Fix**: Only DRAFT and PENDING campaigns can be edited or deleted. Check the campaign status before attempting the operation. PUBLISHED/SENT campaigns are immutable.

### "java.lang.IllegalArgumentException: Invalid status transition: DRAFT → SENT"
- **Cause**: The domain state machine rejected a status transition. Only valid transitions are: `DRAFT → PENDING`, `DRAFT → PUBLISHED`, `PENDING → PUBLISHED`.
- **Fix**: Send a valid target status in the `PATCH /status` request body.

### "org.springframework.dao.DataIntegrityViolationException" / "PSQLException: ERROR: insert or update on table 'campaigns' violates foreign key constraint"
- **Cause**: The `newsletter_id` references a newsletter that does not exist in the `newsletters` table.
- **Fix**: Verify the newsletter UUID exists. This should not happen through the API (ownership verification catches invalid slugs first), but can occur in direct DB operations or tests.

### "Connection to localhost:5432 refused" when running without Docker
- **Cause**: The application expects PostgreSQL at `localhost:5432` (via `application-dev.yml`), but the Docker container is not running.
- **Fix**: Start the database with `docker compose up -d` in the `backend/` directory. For tests, Testcontainers manages its own PostgreSQL — Docker just needs to be running.

### Spotless/PMD/SpotBugs violations blocking the build
- **Cause**: The pre-commit hook or CI enforces formatting and static analysis.
- **Diagnose**: 
  - Format: `./mvnw spotless:apply` auto-fixes most issues.
  - PMD: `./mvnw pmd:check` lists the violations with file and line.
  - SpotBugs: `./mvnw spotbugs:check` reports bug patterns.
- **Fix**: Run `./mvnw spotless:apply` for formatting, then address PMD and SpotBugs warnings manually. Run `./mvnw spotless:check && ./mvnw pmd:check && ./mvnw spotbugs:check` before committing.

## Frontend

### "ƒ describe is not defined" / "ƒ it is not defined" when running tests
- **Cause**: Running `npx vitest run` directly bypasses Angular CLI's test configuration that enables vitest globals.
- **Fix**: Always use `ng test` (or `npm test`) instead of direct `vitest` commands. Angular CLI handles the configuration automatically.

### "Can't bind to 'routerLink' since it isn't a known property"
- **Cause**: `RouterLink` is used in the template but is not imported in the component's `imports` array.
- **Fix**: Add `RouterLink` to the component's `imports` array:
  ```typescript
  imports: [RouterLink, ...]
  ```

### "NG8002: Can't bind to 'formGroup' since it isn't a known property"
- **Cause**: Reactive Forms directives are used without importing `ReactiveFormsModule`.
- **Fix**: Add `ReactiveFormsModule` to the component's `imports` array.

### Unexpected HTTP errors in tests ("Expected one matching request for criteria ... found none")
- **Cause**: An HTTP request was made that wasn't expected by `HttpTestingController`. Either the test forgot to expect the request, or the component made an unexpected call (often from a reload after a successful operation, like deleting a campaign).
- **Diagnose**: Check the error message to see which URL was unexpected. If the test needs a confirm dialog, mock `window.confirm`:
  ```typescript
  vi.spyOn(window, 'confirm').mockReturnValue(true);
  ```
- **Fix**: Ensure all HTTP requests in the tested code path have corresponding `httpMock.expectOne(...)` calls. Call `httpMock.verify()` in `afterEach` to catch unverified requests.

### "Error: Not implemented: Window's confirm()" in test output
- **Cause**: The component calls `window.confirm()` (e.g., in a delete flow), but jsdom does not implement it.
- **Fix**: Mock `confirm` in the test before triggering the action:
  ```typescript
  vi.spyOn(window, 'confirm').mockReturnValue(true);
  ```

### Docker build fails during Maven dependency download
- **Cause**: The Docker build context doesn't include `.m2` cache, so Maven downloads all dependencies from scratch each build.
- **Fix**: The `dependency:go-offline` step is cached by Docker layer caching. If you need to force a clean rebuild: `docker compose -f deploy/docker-compose.yml build --no-cache backend`.

### Docker build fails with Spotless/PMD violations
- **Cause**: Code quality checks run during `mvn package`. In Docker builds, quality is enforced in CI/pre-commit, not in the container.
- **Fix**: The Dockerfile already passes `-Dspotless.check.skip=true`. If adding new checks, verify they don't break the Docker build.

### "nginx: [emerg] host not found in upstream 'backend'" when testing nginx locally
- **Cause**: `nginx -t` outside Docker can't resolve the `backend` hostname (it only exists inside the Docker network).
- **Fix**: Use `docker compose up` to test, or add `--add-host backend:127.0.0.1` for local validation: `nginx -t --add-host backend:127.0.0.1`.

### "Error response from daemon: pull access denied" for notify images
- **Cause**: `image: notify-nginx`, `image: notify-backend`, etc. are local-only images built from Dockerfiles, not pushed to a registry.
- **Fix**: Always use `docker compose -f deploy/docker-compose.yml build` before `up`. The images don't exist remotely.

### Container exits immediately with "oci runtime error"
- **Cause**: Usually a missing or misconfigured `entrypoint`/`cmd` in the Dockerfile.
- **Fix**: For `eclipse-temurin:17-jre-alpine`, the backend uses `ENTRYPOINT ["java", "-jar", "app.jar"]`. Verify the JAR exists at `/app/app.jar` in the container. For frontend, use `nginx -g "daemon off;"` as the default command.

### Prettier/ESLint violations blocking commit
- **Cause**: Husky pre-commit hook runs `lint-staged` which checks formatting and lint on staged files.
- **Diagnose**: The hook output shows which files have violations.
- **Fix**: Run `npm run quality:fix` (from `frontend/`) to auto-fix both Prettier and ESLint issues. Then re-stage and retry the commit:
  ```bash
  cd frontend && npm run quality:fix
  git add <files> && git commit
  ```

### Blank page or "Cannot find module" errors after pulling
- **Cause**: Dependencies changed in `package-lock.json` but `node_modules` is stale.
- **Fix**: Run `npm install` in the `frontend/` directory to sync dependencies.

### Port 4200 already in use
- **Cause**: Another `ng serve` instance is running or the port is occupied.
- **Fix**: Kill the process or use a different port:
  ```bash
  npx ng serve --port 4201
  ```

## Git & Workflow

### "hint: The '.husky/pre-commit' hook was ignored because it's not set as executable"
- **Cause**: Husky hooks need executable permissions.
- **Fix**: 
  ```bash
  chmod +x .husky/pre-commit
  ```

### Commit rejected by pre-commit hook with formatting errors
- **Cause**: `lint-staged` runs format/lint checks and aborts if any fail.
- **Fix**: Stage only the intended files, fix violations, then retry:
  ```bash
  cd frontend && npm run quality:fix
  cd ../backend && ./mvnw spotless:apply
  git add -u
  git commit
  ```

### "There are no stages yet" when trying to commit
- **Cause**: Forgot to `git add` files before committing.
- **Fix**: Stage the intended files with `git add <files>` and retry. Never use `git add .` without reviewing what will be staged first.

### Merge conflicts on generated files (package-lock.json, pom.xml)
- **Cause**: Generated/lock files often conflict when multiple branches modify dependencies.
- **Fix**: Accept both versions and regenerate:
  ```bash
  # For package-lock.json
  git checkout --ours package-lock.json && npm install
  # For pom.xml — resolve manually, then:
  git add pom.xml && git commit
  ```

---

### API Usage Example

Login as admin:

```bash
curl -X POST http://localhost:80/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@notify.com","password":"Admin@123"}'
```

Returns an `AuthResponse` with `accessToken` (15min) and `refreshToken` (7d).

> Em produção (Docker), todas as requisições passam pelo gateway Nginx na porta 80.
> Localmente (dev), a API está na porta 8080 diretamente.

---

# Cross-Cutting Conventions

### Logging

- **Backend**: SLF4J + Logback. Structured logging in JSON format for production.
- **Frontend**: `console` methods only during development. Angular production build removes them.
- No `System.out.println`, no `print()` in committed code.

### Error Handling

- **Backend**: Global `@ControllerAdvice` exception handler. Maps domain exceptions → HTTP Problem Details (RFC 7807).
- **Frontend**: HTTP interceptor catches errors globally. Feature services may add feature-specific error handling.

### Zero Trust Communication

Notify adopts **Zero Trust** for all communication between frontend, backend, internal services, and external systems. No component assumes received data is valid or safe just because it came from another internal component.

#### Core Principles

1. **Never trust, always verify** — every request, message, or event is validated and authorized at the destination, regardless of origin.
2. **Validate at every boundary** — validation happens at each layer (interface, application, domain), not just at the perimeter.
3. **Reject invalid data explicitly** — missing, malformed, or out-of-schema data is rejected with a standardized error, never silently ignored.
4. **Client-side validation is convenience, not security** — the backend never relies on frontend validation.

---

#### Backend — Validation Responsibilities

##### Interface Layer (REST Controllers)

| Validation | Mechanism | Required |
|-----------|-----------|-------------|
| Request format/structure | Jakarta Validation (`@Valid` + annotations on DTO) | Yes |
| Content-Type | `consumes` / `produces` on `@RequestMapping` | Yes |
| Max payload size | `spring.servlet.multipart.max-file-size` + `@Size` | Yes |
| String sanitization | HTML/script injection removal via filter or `StringEscapeUtils` | Yes |
| Client-provided IDs | Validate format (e.g., UUID), then revalidate existence/permission in domain | Yes |

**Mandatory pattern for every endpoint:**

```java
@PostMapping
public ResponseEntity<ResponseDTO> create(
    @Valid @RequestBody RequestDTO request,
    @AuthenticationPrincipal UserPrincipal currentUser
) {
    // request already validated by Jakarta Validation
    // currentUser extracted from JWT token (never trust body for roles/permissions)
    ...
}
```

##### Application Layer (Use Cases)

| Validation | Mechanism |
|-----------|-----------|
| Business rules | Domain Services — validate invariants before persisting |
| Authorization | Verify `currentUser` permissions vs. target resource |
| Resource ownership | Re-query from DB: does the resource belong to the user? |
| Domain limits | E.g., sufficient balance, quota not exceeded, allowed state |

Rules:
- **Never use IDs, roles, permissions, or flags sent by the client** without revalidating on the server. A `userId` in the request body is never trustworthy — always use the `currentUser` extracted from the auth token.
- Every write operation must verify that the authenticated user **has permission** to execute it on that specific resource.

##### Domain Layer

| Validation | Mechanism |
|-----------|-----------|
| Aggregate invariants | Value Objects with self-validation in constructor |
| State limits | Domain methods that reject invalid transitions |
| Intra-aggregate consistency | Rules encapsulated in the aggregate root |

**Value Objects are immutable and self-validated:**

```java
@Embeddable
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new InvalidEmailException("Invalid email format");
        }
        this.value = value.toLowerCase().trim();
    }
}
```

##### Invalid Data Handling

| Situation | HTTP Response | Payload |
|----------|---------------|---------|
| Jakarta validation error | `400 Bad Request` | RFC 7807 Problem Details with field list |
| Malformed data (invalid JSON) | `400 Bad Request` | `{"type":"/errors/malformed-payload","title":"Malformed request body"}` |
| Missing field | `400 Bad Request` | `{"detail":"field 'email' is required"}` |
| Invalid ID format | `400 Bad Request` | `{"detail":"Invalid UUID format for 'userId'"}` |
| Resource not found | `404 Not Found` | `{"detail":"User not found"}` |
| No permission | `403 Forbidden` | `{"detail":"Insufficient permissions"}` |
| Not authenticated | `401 Unauthorized` | — |
| Internal error | `500 Internal Server Error` | No internal details in response |

All validation exceptions are mapped via a global `@ControllerAdvice`.

##### Validation in Events and Messages

- Messages received from queues/streams go through the **same validation schema** as HTTP requests.
- Published events contain only validated data (never unsanitized input).
- Malformed messages are sent to DLQ (Dead Letter Queue) with structured logging.

---

#### Frontend — Validation Responsibilities

##### Validation of Data Received from Backend

| Validation | Mechanism | Required |
|-----------|-----------|-------------|
| Response structure | TypeScript interfaces + runtime type guard (if needed) | Yes |
| Safe rendering | Angular `DomSanitizer` for trusted HTML; `{{ }}` auto-escapes HTML | Yes |
| URLs in dynamic data | `DomSanitizer.sanitize(SecurityContext.URL, value)` before using in links | Yes |
| Missing data | `??` operator with fallback; template `@if` checks null/undefined | Yes |
| Unexpected data | `unknown` type in untyped responses + explicit type guard | Yes |

**Rules:**

- Never use `innerHTML` without `DomSanitizer.sanitize(SecurityContext.HTML, ...)`.
- Never interpolate untrusted data into URLs, styles, or scripts.
- Every API response must be treated as potentially malformed — use fallbacks and error states.

##### Validation of Data Sent to Backend

- **Client-side validation** exists only for user experience (immediate feedback). The backend always revalidates.
- Forms use Angular Reactive Forms with `Validators` for visual feedback.
- The `ApiService` (or feature services) never modifies/enriches user data without explicit validation.

##### Validation Error Handling

| Situation | Behavior |
|----------|---------------|
| 400 — invalid field | Display error message on the corresponding field |
| 400 — malformed payload | Generic error toast/notification + log |
| 401 — not authenticated | Redirect to login |
| 403 — no permission | Toast + redirection if applicable |
| 404 — resource not found | "Not found" screen or fallback |
| 5xx — internal error | "Unexpected error" toast + detailed log |
| Timeout / network off | Visual indicator + automatic retry (if idempotent) |

The `HttpInterceptor` captures globally and decides handling by status code.

##### Validation in WebSockets/SSE

- Messages received via WebSocket are validated with the **same rigor** as HTTP responses.
- Malformed events are discarded with console log in development.
- Never render WebSocket message content directly without sanitization.

---

#### Communication Contracts

##### Payload Format

| Property | Type | Required | Description |
|-------------|------|-------------|-----------|
| `data` | `object \| null` | Yes | Main operation payload |
| `meta` | `Meta \| null` | Yes | Metadata (pagination, version) |
| `errors` | `Error[] \| null` | Yes | Error list (empty on success) |

```json
{
  "data": { "id": "uuid", "name": "..." },
  "meta": { "page": 0, "size": 20, "total": 42 },
  "errors": []
}
```

##### Error Contract (RFC 7807)

```json
{
  "type": "/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "The request contains invalid fields",
  "instance": "/api/v1/users",
  "violations": [
    { "field": "email", "message": "must be a valid email address" },
    { "field": "name", "message": "must not be blank" }
  ]
}
```

##### Endpoint Map (must be updated on every change)

| Method | Path | Request DTO | Response DTO | Validations |
|--------|------|-------------|--------------|------------|
| `GET` | `/api/v1/health` | — | `HealthResponse` | N/A |
| `POST` | `/api/v1/auth/login` | `LoginRequest` | `AuthResponse` | Email + Password not blank |
| `POST` | `/api/v1/auth/register` | `RegisterUserRequest` | `UserResponse` | Name, Email, Password not blank |
| `GET` | `/api/v1/users/me` | — | `UserResponse` | Authenticated |
| `GET` | `/api/v1/newsletter/{slug}` | — | `NewsletterResponse` | — |
| `POST` | `/api/v1/newsletter/{slug}/subscribe` | `SubscribeRequest` | — | Email not blank, valid format |
| `GET` | `/api/v1/newsletter/{slug}/subscribers` | — | `Page<SubscriberResponse>` | Authenticated, owner only, paginated |
| `POST` | `/api/v1/newsletter/{slug}/campaigns` | `CreateCampaignRequest` | `CampaignResponse` (201) | Subject (@NotBlank @Size max 200), Content (@NotBlank @Size max 20000) |
| `GET` | `/api/v1/newsletter/{slug}/campaigns` | — | `Page<CampaignResponse>` | Authenticated, owner only, paginated |
| `GET` | `/api/v1/newsletter/{slug}/campaigns/{id}` | — | `CampaignResponse` | Authenticated, owner only |
| `PUT` | `/api/v1/newsletter/{slug}/campaigns/{id}` | `UpdateCampaignRequest` | `CampaignResponse` | Same as Create + editable status guard |
| `DELETE` | `/api/v1/newsletter/{slug}/campaigns/{id}` | — | 204 No Content | Authenticated, owner only, deletable status guard |
| `PATCH` | `/api/v1/newsletter/{slug}/campaigns/{id}/status` | `CampaignStatusRequest` | `CampaignResponse` | Status enum, valid transition guard |

> **Rule**: every new endpoint must add a line to this map. Any change to payload, query params, or headers of an existing endpoint must be reflected here immediately.

---

#### Between Services (Future)

When communication between services happens (microservices, message brokers, external APIs):
- Each service validates inputs as if they came from an untrusted external source.
- Queue messages have versioned schemas and validation at the consumer.
- Tokens/mTLS in all service-to-service communication.
- Event payloads contain only necessary data (least privilege principle).

---

#### Zero Trust Checklist (for every implementation)

- [ ] Request DTO uses `@Valid` + Jakarta Validation annotations?
- [ ] Controller extracts `@AuthenticationPrincipal` and does not trust IDs from body?
- [ ] Application service verifies resource-specific authorization?
- [ ] Value Objects self-validate in constructor?
- [ ] Frontend treats response as `unknown` until validation?
- [ ] Rendered data goes through Angular sanitization?
- [ ] Validation errors have consistent handling (backend + frontend)?
- [ ] Contracts documented in the "Endpoint Map" section?
- [ ] Events/messages have explicit schema validation?

### Environment Configuration

- **Backend**: `application-{profile}.yml`. Profiles: `dev`, `test`, `prod`.
- **Frontend**: `environments/environment.ts` + `environment.prod.ts`.

---

## Quality & Formatting Stack

### Backend (Java 17 + Maven)

| Tool | Purpose | Command |
|------|---------|---------|
| Spotless | Code formatter (Java, Eclipse JDT) | `./mvnw spotless:check` / `spotless:apply` |
| PMD | Static analysis (best practices, error-prone code) | `./mvnw pmd:check` |
| SpotBugs | Bug pattern detection (null safety, threading, correctness) | `./mvnw spotbugs:check` |
| Combined | All quality checks | `./mvnw spotless:check && ./mvnw pmd:check && ./mvnw spotbugs:check` |

**Rules:**
- `spotless:check` must pass before commit. Auto-fix with `./mvnw spotless:apply`.
- PMD rulesets: `category/java/bestpractices.xml`, `category/java/errorprone.xml`
- SpotBugs effort: `Max`, threshold: `Low` (catches all issues)
- Zero violations allowed on committed code.
- Reports: `target/pmd.xml`, `target/spotbugsXml.xml`

### Frontend (Angular 22 + TypeScript 6.0)

| Tool | Purpose | Command |
|------|---------|---------|
| Prettier | Code formatter (TS, HTML, SCSS, JSON) | `npm run format` / `npm run format:check` |
| ESLint | Static analysis (code quality, Angular best practices) | `npm run lint` |

**Rules:**
- ESLint extends: `angular-eslint` recommended rules
- Prettier config: existing `.prettierrc` (printWidth: 100, singleQuote: true)
- `npm run format:check && npm run lint` must pass before commit
- Zero warnings allowed on committed code.

### Coverage Requirements

| Metric | Threshold | Enforced At |
|--------|-----------|-------------|
| Lines | ≥ 80% | CI (GitHub Actions) |
| Branches | ≥ 70% | CI (GitHub Actions) |
| Functions | ≥ 80% | CI (GitHub Actions) |

Coverage enforced via Vitest config (`coverage.thresholds`) in CI.

---

# Tooling & Dependencies

### Backend (pom.xml / build.gradle)

| Dependency              | Purpose                              |
| ----------------------- | ------------------------------------ |
| Spring Boot Starter Web| REST controllers, embedded Tomcat     |
| Spring Boot Starter Data JPA | Repository support             |
| Spring Boot Starter Validation | Bean Validation (Jakarta)   |
| Spring Boot Starter Security | JWT auth                        |
| Flyway                  | Database migrations                  |
| PostgreSQL Driver       | JDBC driver                          |
| Lombok                  | Boilerplate reduction (optional)     |
| Testcontainers          | PostgreSQL for integration tests     |
| JUnit 5 + AssertJ       | Unit/integration testing             |
| Mockito                 | Mocking                              |

### Frontend (package.json)

| Dependency              | Purpose                        |
| ----------------------- | ------------------------------ |
| @angular/core           | Framework                      |
| @angular/router         | Client-side routing            |
| @angular/forms          | Reactive forms                 |
| @angular/common/http    | HTTP client                    |
| Vitest                  | Testing (Angular 22 default)   |

---

This document is live. Update it when architectural decisions change.

---

## Decision Framework

### Fundamental Rule

> **Every task — frontend, backend, infrastructure — must be approved by the user before execution.**
>
> No architecture decision, component, library, theme, layout, endpoint, migration, or configuration is assumed by the planner or implementer. Every choice must be explicitly consulted and approved.

### Mandatory Questions Checklist

#### Frontend (Angular 22 + Angular Material)

**1. Component and Variant**
- Which Angular Material component meets this requirement? (from `@angular/material`)
- If no ready component exists, what is the approach? (custom using CDK, composition, etc.)
- Which variant? Color (`primary`/`accent`/`warn`), appearance (`raised`/`stroked`/`flat`/`icon`), state (`disabled`/`readonly`/`error`/`loading`)

**2. Theme and Style**
- Current theme is light, dark, or both? Default indigo/pink or custom palette?
- Uses Material design tokens (color/typography/elevation) or custom CSS values?
- Any spacing/radius values outside Material defaults?
- Global or encapsulated style? (`:host` vs `styles.scss`)

**3. Layout and Spacing**
- What layout? Cards, containers, lists, grid, flex? Angular Flex Layout or CSS Grid/Flexbox?
- Spacing follows the design system or is specific?
- Which breakpoints are relevant? (Standard CSS media queries or `BreakpointObserver`)

**4. Forms and Inputs**
- Template-driven or Reactive Forms?
- How to display validation errors? `<mat-error>` or custom? (related to backend RFC 7807)
- Input masks needed?
- Input sizes: default, dense?

**5. Navigation and Routes**
- Which navigation components? Tabs (`<mat-tab-group>`), breadcrumbs, pagination (`<mat-paginator>`)?
- Page layout: sidebar + content (`<mat-sidenav>`), header + content, etc.?
- Loading/empty/error state for each route?

**6. Data and Tables**
- Simple table or with paginated requests? (`<mat-table>` from `@angular/material/table`)
- Sorting, filtering, search? (`<mat-sort>`, `<mat-form-field>` for filter)
- Empty and loading states?

**7. Icons and Assets**
- Material Icons (`<mat-icon>`) or custom SVGs?
- Default icon font? (Material Icons or Font Awesome)

**8. Accessibility**
- Required aria/label attributes?
- Keyboard navigation support?
- Color contrast verified?

**9. Frontend Tests**
- Component test with Angular Material? (`TestBed` with necessary Material module imports)
- Interaction test (click, input, submission)?
- Snapshot or computed value?

#### Backend (Java 17 + Spring Boot 4.0.6 + DDD)

**10. Bounded Context**
- Which bounded context does this endpoint/feature belong to?
- Does it already exist or needs to be created?

**11. Endpoint**
- Which HTTP method and path? (`/api/v1/{resource}`)
- Request DTO exists or needs to be created?
- Response DTO exists or needs to be created?
- Required Jakarta Validations? (`@NotBlank`, `@Email`, `@Size`, etc.)
- Query params or pagination?

**12. Domain**
- New aggregate root, entity, or value object?
- Which business invariants?
- Which Zero Trust rules apply? (see the Zero Trust section checklist)

**13. Persistence**
- Flyway migration needed?
- New JPA repository?
- Relationships: `@ManyToOne`, `@OneToMany`?

**14. Security and Authorization**
- Is the endpoint public or authenticated?
- Who can access it? (roles, permissions)
- Resource ownership verification?

**15. Backend Tests**
- Unit test (domain, without Spring)?
- Integration test (controller, `@SpringBootTest RANDOM_PORT`)?
- Persistence test (`@DataJpaTest` + Testcontainers)?

#### Infrastructure

**16. Dependencies**
- New Maven or npm dependency?
- Version compatible with the current stack?

**17. Configuration**
- New environment variable or config?
- Profile: `dev`, `test`, `prod`?

**18. Database**
- New migration? File name `V{number}__{description}.sql`
- Rollback planned?

### Approval Flow

```
[Identified need]
       │
       ▼
[Prometheus prepares task-specific questions]
       │
       ▼
[User responds and approves decisions]
       │
       ▼
[Plan is generated with all decisions recorded]
       │
       ▼
[User approves the plan → /start-work]
```

> **Note**: Concrete design system decisions (colors, dark mode, typography, icons) will be defined per task, on demand, following this same flow.

<!-- ai-memory:start -->
## Long-term memory (ai-memory)

This project uses [ai-memory](https://github.com/akitaonrails/ai-memory)
for cross-session continuity.

**Default to the current project — always.** Every ai-memory tool
auto-scopes to the project resolved from your session's working
directory. **Do NOT pass `project`, `workspace`, or `cwd` arguments unless the user
explicitly references a *different* project by name** (e.g. "what did we
decide in the `other-app` project?"). Phrases like "this project",
"here", "we", "our work", "where did we leave off" all mean the *current*
project — call the tool with no scoping args. If the user asks about a
handoff and the SessionStart auto-fetched block is already in your
context, just answer from it; do not re-call the tool to "find it again"
in another project.

**Lifecycle hooks already capture every prompt + tool call
automatically.** You never need to manually write routine notes; the
SessionStart hook auto-fetches pending handoffs, and on session end
ai-memory writes a session-summary page and a handoff.
LLM consolidation (compiling observations into topical wiki pages) runs
on PreCompact, on demand via `memory_consolidate`, and at session end
only when the server sets `AI_MEMORY_CONSOLIDATE_ON_SESSION_END`. Only
write a durable wiki page when the user explicitly asks to remember or
annotate something permanently.

### When to reach for each tool

The user can express any of the intents below in plain English —
match the intent to the tool. They do not need to name the tool.

| User says / situation | Tool |
|---|---|
| "have we discussed X?" / "search memory for Y" / before proposing architecture | `memory_query` (current project; `scopes` for named siblings; `global=true` to search every project) |
| "what's been going on" / "show recent activity" (light) | `memory_recent` |
| "is ai-memory healthy?" / "how big is the wiki?" | `memory_status` |
| "give me the stats" / structured snapshot for the agent to consume | `memory_briefing` (read-only; never creates handoffs) |
| "catch me up" / "I've been away" / "what's important right now?" / open-ended exploration | `memory_explore` |
| "where did we leave off?" — and you see a `📥 ai-memory: pending handoff` block in your context | already done — answer from that block; do NOT re-call `memory_handoff_accept` |
| "where did we leave off?" — and no such block is visible | `memory_handoff_accept` (rare; the SessionStart hook usually got there first; pass `workspace` + `project` together only for a named sibling workspace/project) |
| "save context for the next session" / wrapping up / ending this session | `memory_handoff_begin` (session-end only; do **not** use for status/briefing; single-use handoff; terse summary; put detail in `open_questions` + `next_steps` bullets; pass `workspace` + `project` together only for a named sibling workspace/project) |
| "discard that handoff" / "I created a handoff by mistake" | `memory_handoff_cancel` (requires exact `handoff_id` from `memory_handoff_begin`; marks it expired before the next session sees it) |
| "consolidate this session" / "compile what we learned" (also runs on PreCompact; at session end only if `AI_MEMORY_CONSOLIDATE_ON_SESSION_END` is set) | `memory_consolidate` |
| "what did we learn from this session?" / "what memory should we add?" / explicit wrap-up learning review | `memory_auto_improve` (manual learning review for a completed session; omit `session_id` for latest completed session; the server also schedules background review for newly completed sessions in every project when configured) |
| "remember this permanently" / "save a note" / "add an annotation" / durable project knowledge | `memory_write_page` (write a wiki page; do **not** use handoff for permanent notes; put the title as a `# H1` on the first line of `body` and omit the `title` arg — ai-memory derives it from the H1) |
| "read the page about X" / "show me the full content of Y" / "open the page on Z" | `memory_read_page` (full body; pass a query to search or `path` for a direct lookup; pass `workspace` + `project` together only for a named sibling workspace/project) |
| "delete the page X" / "remove that note" | `memory_delete_page` (by exact `path`; idempotent; pass `workspace` + `project` together only for a named sibling workspace/project) |
| "audit the wiki" / "find contradictions" / "what rules should we add?" | `memory_lint` |
| "prune old pages" / "memory cleanup" | `memory_forget_sweep` |

`memory_explore` is the right default for the "I want to know what's
going on" use case — it returns a prose digest whose verbosity
scales automatically to how long it's been since the last activity
(< 1 h → one line; > 30 days → full catchup).

### When the current project comes up empty — broaden the search

`memory_query` searches only the **current** project by default. If a
search comes back empty or thin, the knowledge may live in a **sibling
project** — shared `infra`, `ops`, or a related app. Don't conclude
"we never recorded it" after a single project misses; broaden instead:

- **Know which projects to check?** Re-run with explicit `scopes`, e.g.
  `scopes: [{ "workspace": "default", "project": "infra" }]`.
- **Don't know where it lives?** Pass `global=true` to search every
  project in every workspace at once. Each hit is annotated with its
  workspace + project so you can tell where it came from. `global=true`
  cannot be combined with `scopes`/`project`/`workspace`.

`memory_query` returns **snippets, not full page bodies** — an empty or
short snippet does **not** mean the page is empty (a large page can
match outside the snippet window). To read the whole page, use
`memory_read_page` (by `path`, or pass a `query` to fetch the top hit's
full body; add `workspace` + `project` together only when the user names
a sibling workspace/project).

### Use Retrieved Memory As Operating Guidance

When `memory_query` or `memory_recent` returns `_rules/`, `gotchas/`,
`procedures/`, or `decisions/` pages that match the current task, treat
them as actionable context, not trivia:

- Read full pages with `memory_read_page` when the snippet looks relevant.
- Apply `_rules/` as constraints.
- Check `gotchas/` as preflight warnings before editing the same subsystem.
- Follow `procedures/` as checklists for releases, PR reviews, deploys,
  migrations, and other repeatable workflows.
- Use `decisions/` as prior architecture unless the user explicitly asks
  to revisit them.

Before non-trivial coding, debugging, deployment, release, auth, scope,
migration, PR-review, or data-preservation work, search memory for the
subsystem and task type first. If the first query is thin, broaden or
query specific error/subsystem terms before designing a fix.

### Learning Review

The server schedules background auto-improvement for newly completed sessions in
every project when an LLM provider is configured. `memory_auto_improve` is the manual version:
use it when the user asks what durable lessons this session suggests, or at
explicit wrap-up when reviewing proposed memory would be useful. Scheduled and
manual runs apply or stage validated edits through the auto-improvement approval
path. Admins can turn off scheduling with `[auto_improve.scheduler] enabled =
false`, or opt into manual proposal approval with `[auto_improve]
require_approval = true`, in which case scheduled and manual proposals stay in
pending-writes until approved.

### When you write a project rule, write it here

If you're about to write a durable project rule ("always X", "never
Y", "all PRs must …"), write it in the project's canonical agent
instruction file. Many projects use CLAUDE.md for Claude Code and
AGENTS.md for Codex / OpenCode / Cursor / Gemini CLI, but if the
project says one file is canonical, use that file. ai-memory's lint
pass surfaces the same hint automatically when a `kind: rule` page
lands in `_rules/`.

### Refreshing this snippet

This block is maintained by ai-memory. Two ways to refresh it with
the latest binary's recommended copy:

- **From the agent** (no terminal needed): ask "refresh the ai-memory
  routing in this project" — the agent calls
  `memory_install_self_routing`, picks the right filename for itself
  (Claude Code → `CLAUDE.md`; Codex / OpenCode / Cursor / Gemini →
  `AGENTS.md`), and uses its Write / Edit tool to land the block.
- **From the CLI**: `ai-memory install-instructions` (defaults to
  `CLAUDE.md`; pass `--target AGENTS.md` for non-Claude agents or
  projects that use `AGENTS.md` as the canonical instruction file).

Both are idempotent: re-runs replace the block bracketed by
`<!-- ai-memory:start -->` / `<!-- ai-memory:end -->` markers
without disturbing the rest of the file.
<!-- ai-memory:end -->

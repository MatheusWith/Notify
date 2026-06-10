# Notify — Architecture & Conventions

## Stack Overview

| Layer     | Technology                                          |
| --------- | --------------------------------------------------- |
| Backend   | Java 17 · Spring Boot 4.0.6 · JAR packaging         |
| Frontend  | Angular 22 · TypeScript 6.0                         |
| Database  | PostgreSQL                                          |
| Approach  | Domain-Driven Design (DDD) · Test-Driven Dev (TDD)  |

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
│   │   │   │   │   └── mapper/      # DTO <-> Domain mappers
│   │   │   │   ├── infrastructure/  # Persistence, messaging, external APIs
│   │   │   │   │   ├── repository/  # JPA / repository implementations
│   │   │   │   │   ├── mapper/      # JPA Entity <-> Domain Entity mappers
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
```

## Architecture: Layered DDD

```
┌──────────────────────────────────────┐
│        Interfaces (REST/WS)          │  Spring Controllers — thin, delegates to Application
├──────────────────────────────────────┤
│       Application (Use Cases)        │  Orchestrates domain objects, manages transactions
├──────────────────────────────────────┤
│          Domain (Core)               │  Entities + VOs + Domain Services — anotados com Lombok, JPA e Jakarta Validation
│   Entities · Value Objects · Events  │  Single source of truth for business rules
├──────────────────────────────────────┤
│      Infrastructure (Persistence)    │  JPA repositories, external clients, messaging
└──────────────────────────────────────┘
```

### Rules

- **Domain objects** usam **Lombok** (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) para reduzir boilerplate, **JPA** (`@Entity`, `@Table`, `@Id`, `@Embeddable`, etc.) para mapeamento ORM, e **Jakarta Validation** (`@NotBlank`, `@NotNull`, `@Email`, etc.) para validação de entrada.
- **Domain layer** pode usar Spring annotations (`@Service` em serviços de domínio) quando necessário — mas mantém zero dependência de frameworks externos (mensageria, API clients, etc.).
- **Repositories** são interfaces Spring Data JPA (`extends JpaRepository`) ou interfaces customizadas, colocadas em `infrastructure/repository/`. Se preferir, também podem ficar em `domain/repository/` com implementação em `infrastructure/`.
- **Application Services** usam `@Service`, `@Transactional` e coordenam objetos de domínio, gerenciam transações e publicam eventos. São a fronteira dos casos de uso do sistema.
- **Controllers** são finos — usam `@RestController`, `@RequestMapping`, etc. Parseiam input → chamam application service → retornam DTO. Sem lógica de negócio.
- **Injeção de dependência** é por construtor (com `@RequiredArgsConstructor` do Lombok). Sem field injection.

### DDD Tactical Patterns

- **Aggregates**: Cluster of entities/values treated as a unit. One aggregate = one transaction boundary. Reference other aggregates only by ID.
- **Value Objects**: Immutable, self-validating on construction. Anotados com `@Embeddable`. No setters. Replaceable, not changeable.
- **Domain Events**: Record what happened (`UserRegistered`, `NotificationSent`). Published by application services after domain operations complete.
- **Repositories**: Collection-like interface per aggregate root. `findById`, `save`, `delete` — never expose raw data access.

### Annotations Reference

| Anotação            | Onde usar                              | Propósito                                    |
| ------------------- | -------------------------------------- | -------------------------------------------- |
| **Lombok**          |                                        |                                              |
| `@Data`             | Entidades, VOs, DTOs                   | `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` + `@RequiredArgsConstructor` |
| `@Builder`          | Entidades, VOs, DTOs                   | Construtor fluido (Builder pattern)          |
| `@NoArgsConstructor`| Entidades JPA                          | Construtor vazio (obrigatório JPA)           |
| `@AllArgsConstructor` | DTOs, VOs                            | Construtor com todos os parâmetros           |
| `@RequiredArgsConstructor` | Services, Controllers          | Construtor com campos `final` para DI        |
| `@Slf4j`            | Services, Controllers                  | Logger SLF4J automático                      |
| **JPA (Jakarta Persistence)** |                                |                                              |
| `@Entity`           | Classes do domínio                     | Mapeamento ORM — entidade do banco           |
| `@Table`            | Classes `@Entity`                      | Personaliza nome da tabela                   |
| `@Id`               | Campo da entidade                      | Chave primária                               |
| `@GeneratedValue`   | Campo da chave primária                | Estratégia de geração de ID                  |
| `@Column`           | Campos da entidade                     | Personaliza nome/restrições da coluna        |
| `@Embeddable`       | Value Objects                          | VO embutido na tabela da entidade            |
| `@Embedded`         | Campo que recebe um `@Embeddable`      | Indica um VO embutido                        |
| `@Enumerated`       | Campo enum                             | Mapeamento de enum (`ORDINAL` ou `STRING`)   |
| `@ManyToOne` / `@OneToMany` | Relacionamentos entre entidades | Cardinalidade do relacionamento              |
| `@JoinColumn`       | Lado dono do relacionamento            | Personaliza FK                               |
| **Spring**          |                                        |                                              |
| `@Service`          | Application / Domain Services          | Bean de serviço (camada de negócio)          |
| `@Repository`       | Interfaces de repositório              | Bean de repositório + tradução de exceções   |
| `@RestController`   | Controllers REST                       | Bean controller + `@ResponseBody`            |
| `@RequestMapping`   | Classe controller                      | Prefixo de rota (ex: `/api/v1/users`)        |
| `@GetMapping` / `@PostMapping` / etc | Métodos do controller | Mapeamento de endpoint HTTP               |
| `@Transactional`    | Application Services / Repositories     | Gerenciamento de transação                   |
| `@RequiredArgsConstructor` | Services, Controllers (Lombok)   | DI via construtor automático                 |
| `@ControllerAdvice` | Manipulador global de erros            | Tratamento centralizado de exceções          |
| `@ExceptionHandler` | Métodos em `@ControllerAdvice`         | Mapeia exceção → HTTP response               |
| `@Configuration`    | Classes de configuração                | Define beans / configuração Spring           |
| **Jakarta Validation** |                                       |                                              |
| `@NotBlank` / `@NotNull` | Campos de DTOs / Entidades        | Validação de entrada                         |
| `@Email`            | Campos de email                        | Valida formato de email                      |
| `@Size`             | Campos string/collection               | Valida tamanho mínimo/máximo                 |
| `@Valid`            | Parâmetro de controller                | Ativa validação no DTO de entrada            |

### Naming Conventions

- Classes: `PascalCase` — `UserRegistrationService`, `NotificationRepository`
- Methods: `camelCase` — `findById`, `registerUser`
- Constants: `UPPER_SNAKE_CASE` — `MAX_RETRY_COUNT`
- Packages: `lowercase` with dot notation — `com.notify.user.domain.model`
- DTOs: `{UseCase}Request` / `{UseCase}Response` — `RegisterUserRequest`, `UserResponse`
- Mappers: `{Source}To{Target}Mapper` — `UserToUserResponseMapper`

### Testing (TDD)

**All code is written test-first.** This is not negotiable.

| Layer         | Test Type         | Annotations / Tools                                       |
| ------------- | ----------------- | --------------------------------------------------------- |
| Domain        | Unit              | JUnit 5 · AssertJ · Mockito                               |
| Application   | Unit + Integration| JUnit 5 · `@SpringBootTest` (only sliced if possible)     |
| Infrastructure| Integration       | `@DataJpaTest` · `@Testcontainers`                        |
| Interfaces    | Integration       | `@SpringBootTest(RANDOM_PORT)` · RestTemplate             |

> **Nota sobre Spring Boot 4.0**: Diferente do Spring Boot 3.x, o 4.0 removeu `@WebMvcTest`, `@AutoConfigureMockMvc`, e `TestRestTemplate`. Testes de controllers usam `@SpringBootTest` com `webEnvironment = RANDOM_PORT` e `RestTemplate` ou `WebTestClient` (adicionando `spring-boot-starter-webflux` como dependência de teste).

- **Domain tests** não carregam o Spring context quando testam regras de negócio puras. JUnit + AssertJ são suficientes. Testes que envolvem persistência ou integração podem usar slices do Spring com `@DataJpaTest`.
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

### API Design

- RESTful over JSON.
- Base URL: `/api/v1/{resource}`.
- Standard HTTP methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.
- Response envelope (consistent): `{ data, meta, errors }`.
- Pagination: `GET /api/v1/{resource}?page=0&size=20&sort=field,asc`.
- Error responses follow RFC 7807 (Problem Details).
- Validation: Jakarta Bean Validation on request DTOs.

### Dependency Injection

- Constructor injection exclusively. Use `@RequiredArgsConstructor` do Lombok para gerar automaticamente o construtor com parâmetros para todos os campos `final`.
- No field injection (`@Autowired` em campos). Nunca.
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
- No Sonar / lint critical issues.
- PR title describes the change, not the ticket number.
- PR body includes: **What** changed, **Why**, **How to test**.

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

[optional body — explain WHAT and WHY, not HOW]
```

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
fix(notification): handle null recipient on send
test(user): cover duplicate email registration
```

### TDD Cycle (Red-Green-Refactor)

1. **Red** — Write a failing test that describes the desired behavior
2. **Green** — Write the minimal production code to make it pass
3. **Refactor** — Improve code quality while keeping tests green

Never write production code without a preceding failing test. Never delete or `@Disabled` a failing test to make CI pass — fix the code.

---

# Running the Application

## Backend (Spring Boot)

### Pré-requisitos

- Java 17+
- Docker (para o PostgreSQL)

### Banco de Dados

```bash
cd backend
docker compose up -d
```

Sobe um PostgreSQL 16 na porta `5432`, database `notify`, user/senha `notify`.

### Executar

```bash
cd backend
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O profile `dev` é ativado por padrão, lendo `application-dev.yml`. O Flyway executa as migrations automaticamente na inicialização.

### Testes

```bash
cd backend
./mvnw clean test
```

Usa **Testcontainers** — sobe um PostgreSQL temporário em Docker automaticamente. Não precisa do `docker compose` local para rodar os testes.

Atualmente **19 testes** de integração (AuthControllerTest, UserControllerTest, HealthControllerTest).

### Dados de Seed

A migration `V1__create_identity_tables.sql` cria automaticamente:

| Email | Senha | Role |
|---|---|---|
| `admin@notify.com` | `Admin@123` | ADMIN |

---

## Frontend (Angular 22)

### Pré-requisitos

- Node.js 22+
- npm 11+

### Executar

```bash
cd frontend
npm install    # apenas na primeira vez
npm start      # ng serve
```

Sobe em `http://localhost:4200`. Por padrão redireciona para `/auth/login`.

### Testes

```bash
cd frontend
npm test
```

Usa **Vitest** com jsdom — não precisa de navegador. Testes unitários de componentes e serviços.

Atualmente **26 testes** (AppComponent, ApiService, LoginPage, RegisterPage).

---

### Exemplo de Uso (API)

Login como admin:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@notify.com","password":"Admin@123"}'
```

Retorna um `AuthResponse` com `accessToken` (15min) e `refreshToken` (7d).

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

O Notify adota o princípio de **Zero Trust** para toda comunicação entre frontend, backend, serviços internos e sistemas externos. Nenhuma parte assume que os dados recebidos são válidos ou seguros apenas porque vieram de um componente interno.

#### Princípios Fundamentais

1. **Nunca confiar, sempre verificar** — toda requisição, mensagem ou evento é validado e autorizado no destino, independentemente da origem.
2. **Validar em toda fronteira** — a validação ocorre em cada camada (interface, application, domain), não apenas no perímetro.
3. **Rejeitar dados inválidos explicitamente** — dados ausentes, malformados ou fora de schema são rejeitados com erro padronizado, nunca silenciosamente ignorados.
4. **Client-side validation é conveniência, não segurança** — o backend nunca depende de validação feita no frontend.

---

#### Backend — Responsabilidades de Validação

##### Camada de Interfaces (REST Controllers)

| Validação | Mecanismo | Obrigatório |
|-----------|-----------|-------------|
| Formato/estrutura da request | Jakarta Validation (`@Valid` + anotações no DTO) | Sim |
| Content-Type | `consumes` / `produces` no `@RequestMapping` | Sim |
| Tamanho máximo de payload | `spring.servlet.multipart.max-file-size` + `@Size` | Sim |
| Sanitização de strings | Remoção de HTML/script injection via filtro ou `StringEscapeUtils` | Sim |
| IDs fornecidos pelo cliente | Validar formato (ex: UUID), depois revalidar existência/permissão no domínio | Sim |

**Padrão obrigatório para todo endpoint:**

```java
@PostMapping
public ResponseEntity<ResponseDTO> create(
    @Valid @RequestBody RequestDTO request,
    @AuthenticationPrincipal UserPrincipal currentUser
) {
    // request já validado por Jakarta Validation
    // currentUser extraído do token JWT (nunca confiar no body para roles/permissions)
    ...
}
```

##### Camada de Application (Use Cases)

| Validação | Mecanismo |
|-----------|-----------|
| Regras de negócio | Domain Services — validam invariantes antes de persistir |
| Autorização | Verificar permissões do `currentUser` vs. recurso alvo |
| Propriedade do recurso | Re-query no DB: o recurso pertence ao usuário? |
| Limites de domínio | Ex: saldo suficiente, quota não excedida, estado permitido |

Regras:
- **Nunca usar IDs, roles, permissões ou flags enviados pelo cliente** sem revalidar no servidor. Um `userId` vindo no body da request nunca é confiável — usar sempre o `currentUser` extraído do token de autenticação.
- Toda operação de escrita deve verificar se o usuário autenticado **tem direito** de executá-la naquele recurso específico.

##### Camada de Domínio

| Validação | Mecanismo |
|-----------|-----------|
| Invariantes do agregado | Value Objects com auto-validação no construtor |
| Limites de estado | Métodos de domínio que rejeitam transições inválidas |
| Consistência intra-agregado | Regras encapsuladas no aggregate root |

**Value Objects são imutáveis e auto-validados:**

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

##### Tratamento de Dados Inválidos

| Situação | Resposta HTTP | Payload |
|----------|---------------|---------|
| Erro de validação Jakarta | `400 Bad Request` | RFC 7807 Problem Details com lista de campos |
| Dado malformado (JSON inválido) | `400 Bad Request` | `{"type":"/errors/malformed-payload","title":"Malformed request body"}` |
| Campo ausente | `400 Bad Request` | `{"detail":"field 'email' is required"}` |
| ID inválido (formato) | `400 Bad Request` | `{"detail":"Invalid UUID format for 'userId'"}` |
| Recurso não encontrado | `404 Not Found` | `{"detail":"User not found"}` |
| Sem permissão | `403 Forbidden` | `{"detail":"Insufficient permissions"}` |
| Não autenticado | `401 Unauthorized` | — |
| Erro interno | `500 Internal Server Error` | Sem detalhes internos no response |

Todas as exceções de validação são mapeadas via `@ControllerAdvice` global.

##### Validação em Eventos e Mensagens

- Mensagens recebidas de filas/streams passam pelo **mesmo schema de validação** que requests HTTP.
- Eventos publicados contêm apenas dados validados (nunca input não sanitizado).
- Mensagens malformadas são enviadas para DLQ (Dead Letter Queue) com log estruturado.

---

#### Frontend — Responsabilidades de Validação

##### Validação de Dados Recebidos do Backend

| Validação | Mecanismo | Obrigatório |
|-----------|-----------|-------------|
| Estrutura da response | Typescript interfaces + runtime type guard (se necessário) | Sim |
| Renderização segura | Angular `DomSanitizer` para HTML confiável; `{{ }}` auto-escapa HTML | Sim |
| URLs em dados dinâmicos | `DomSanitizer.sanitize(SecurityContext.URL, value)` antes de usar em links | Sim |
| Dados ausentes | Operador `??` com fallback; template `@if` verifica null/undefined | Sim |
| Dados inesperados | Tipo `unknown` em responses não tipadas + type guard explícito | Sim |

**Regras:**

- Nunca usar `innerHTML` sem `DomSanitizer.sanitize(SecurityContext.HTML, ...)`.
- Nunca interpolar dados não confiáveis em URLs, estilos ou scripts.
- Toda response da API deve ser tratada como potencialmente malformada — usar fallbacks e estado de erro.

##### Validação de Dados Enviados ao Backend

- **Client-side validation** existe apenas para experiência do usuário (feedback imediato). O backend sempre revalida.
- Formulários usam Angular Reactive Forms com `Validators` para feedback visual.
- O `ApiService` (ou feature services) nunca modifica/enriquece dados do usuário sem validação explícita.

##### Tratamento de Erros de Validação

| Situação | Comportamento |
|----------|---------------|
| 400 — campo inválido | Exibir mensagem de erro no campo correspondente |
| 400 — payload malformado | Toast/notificação de erro genérico + log |
| 401 — não autenticado | Redirect para login |
| 403 — sem permissão | Toast + redirecionamento se aplicável |
| 404 — recurso não encontrado | Tela de "não encontrado" ou fallback |
| 5xx — erro interno | Toast de "erro inesperado" + log detalhado |
| Timeout / rede off | Indicador visual + retry automático (se idempotente) |

O `HttpInterceptor` captura globalmente e decide o tratamento por status code.

##### Validação em WebSockets/SSE

- Mensagens recebidas via WebSocket são validadas com o **mesmo rigor** que responses HTTP.
- Eventos malformados são descartados com log no console de desenvolvimento.
- Nunca renderizar diretamente conteúdo de mensagens WebSocket sem sanitização.

---

#### Contratos de Comunicação

##### Formato de Payload

| Propriedade | Tipo | Obrigatório | Descrição |
|-------------|------|-------------|-----------|
| `data` | `object \| null` | Sim | Payload principal da operação |
| `meta` | `Meta \| null` | Sim | Metadados (paginação, versão) |
| `errors` | `Error[] \| null` | Sim | Lista de erros (vazio em sucesso) |

```json
{
  "data": { "id": "uuid", "name": "..." },
  "meta": { "page": 0, "size": 20, "total": 42 },
  "errors": []
}
```

##### Contrato de Erro (RFC 7807)

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

##### Mapa de Endpoints (deve ser atualizado a cada alteração)

| Método | Path | Request DTO | Response DTO | Validações |
|--------|------|-------------|--------------|------------|
| `GET` | `/api/v1/health` | — | `HealthResponse` | N/A |

> **Regra**: todo novo endpoint deve adicionar uma linha neste mapa. Toda alteração em payload, query params ou cabeçalho de endpoint existente deve ser refletida aqui imediatamente.

---

#### Entre Serviços (Futuro)

Quando houver comunicação entre serviços (microservices, message brokers, APIs externas):
- Cada serviço valida entradas como se fossem de origem externa não confiável.
- Mensagens em fila têm schema versionado e validação no consumidor.
- Tokens/mTLS em toda comunicação entre serviços.
- Payloads de eventos contêm apenas dados necessários (princípio do menor privilégio).

---

#### Checklist de Zero Trust (para toda implementação)

- [ ] Request DTO usa `@Valid` + anotações Jakarta Validation?
- [ ] Controller extrai `@AuthenticationPrincipal` e não confia em IDs do body?
- [ ] Application service verifica autorização específica do recurso?
- [ ] Value Objects auto-validam no construtor?
- [ ] Frontend trata response como `unknown` até validação?
- [ ] Dados renderizados passam por sanitização Angular?
- [ ] Erros de validação têm tratamento consistente (backend + frontend)?
- [ ] Contratos foram documentados na seção "Mapa de Endpoints"?
- [ ] Eventos/mensagens têm validação de schema explícita?

### Environment Configuration

- **Backend**: `application-{profile}.yml`. Profiles: `dev`, `test`, `prod`.
- **Frontend**: `environments/environment.ts` + `environment.prod.ts`.

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

### Regra Fundamental

> **Toda tarefa — frontend, backend, infraestrutura — deve ser aprovada pelo usuário antes da execução.**
>
> Nenhuma decisão de arquitetura, componente, biblioteca, tema, layout, endpoint, migration, ou configuração é assumida pelo planejador ou implementador. Toda escolha deve ser explicitamente consultada e aprovada.

### Checklist de Perguntas Obrigatórias

#### Frontend (Angular 22 + Taiga UI v5)

**1. Componente e Variante**
- Qual componente Taiga UI atende a este requisito? (de `@taiga-ui/kit` ou `@taiga-ui/core`)
- Se não existir componente pronto, qual a abordagem? (custom com CDK, composição, etc.)
- Qual variante? Tamanho (`s`/`m`/`l`), aparência (`primary`/`secondary`/`flat`/`outline`), estado (`disabled`/`readonly`/`error`/`loading`)

**2. Tema e Estilo**
- Tema atual é light, dark, ou ambos? O componente precisa responder a `tuiTheme="dark"`?
- Usa cores do design system (`--tui-background-accent-1`, `--tui-text-primary`) ou customizadas?
- Algum valor de spacing/radius/font fora do padrão Taiga? (`--tui-radius-s/m`, `--tui-height-l/m/s`, `--tui-font-text-*`)
- Estilo global ou encapsulado no componente? (`:host` vs `styles.scss`)

**3. Layout e Espaçamento**
- Qual o layout? Cards, containers, listas, grid, flex? Componentes de layout Taiga?
- Espaçamento segue o design system ou é específico?
- Quais breakpoints são relevantes? (`@tui-mobile`, `@tui-tablet`, `@tui-desktop`)

**4. Formulários e Inputs**
- Template-driven ou Reactive Forms?
- Como exibir erros de validação? Padrão Taiga ou custom? (relacionado ao backend RFC 7807)
- Máscaras de input necessárias? (`@maskito/angular`, `@maskito/kit`)
- Tamanho dos inputs: `s`/`m`/`l`?

**5. Navegação e Rotas**
- Que componentes de navegação? Tabs, breadcrumbs, stepper, pagination?
- Layout da página: sidebar + content, header + content, etc.?
- Estado de loading/empty/error para cada rota?

**6. Dados e Tabelas**
- Tabela simples ou com requisições paginadas? (`<tui-table>` do `@taiga-ui/addon-table`)
- Ordenação, filtro, busca?
- Estado vazio e carregamento?

**7. Ícones e Assets**
- Pacote de ícones Taiga UI? (`@taiga-ui/icons`) Ou ícones customizados (SVG próprios)?
- Tamanho padrão de ícones? `s`/`m`/`l`?

**8. Acessibilidade**
- Atributos aria/label obrigatórios?
- Suporte a navegação por teclado?
- Contraste de cores verificado?

**9. Testes de Frontend**
- Teste de componente com Taiga UI? (`TestBed` com imports necessários)
- Teste de interação (click, input, submissão)?
- Snapshot ou valor computado?

#### Backend (Java 17 + Spring Boot 4.0.6 + DDD)

**10. Bounded Context**
- Este endpoint/feature pertence a qual bounded context?
- Já existe ou precisa ser criado?

**11. Endpoint**
- Qual método HTTP e path? (`/api/v1/{recurso}`)
- Request DTO existe ou precisa ser criado?
- Response DTO existe ou precisa ser criado?
- Validações Jakarta necessárias? (`@NotBlank`, `@Email`, `@Size`, etc.)
- Query params ou paginação?

**12. Domínio**
- Novo aggregate root, entidade, ou value object?
- Quais invariantes de negócio?
- Quais regras de Zero Trust se aplicam? (ver checklist da seção Zero Trust)

**13. Persistência**
- Migration Flyway necessária?
- Novo repositório JPA?
- Relacionamentos: `@ManyToOne`, `@OneToMany`?

**14. Segurança e Autorização**
- Endpoint é público ou autenticado?
- Quem pode acessar? (roles, permissões)
- Verificação de propriedade do recurso?

**15. Testes de Backend**
- Teste de unidade (domínio, sem Spring)?
- Teste de integração (controller, `@SpringBootTest RANDOM_PORT`)?
- Teste de persistência (`@DataJpaTest` + Testcontainers)?

#### Infraestrutura

**16. Dependências**
- Nova dependência Maven ou npm?
- Versão compatível com o stack atual?

**17. Configuração**
- Nova variável de ambiente ou config?
- Profile: `dev`, `test`, `prod`?

**18. Banco de Dados**
- Nova migration? Nome do arquivo `V{numero}__{descricao}.sql`
- Rollback planejado?

### Fluxo de Aprovação

```
[Necessidade identificada]
       │
       ▼
[Prometheus prepara perguntas específicas da tarefa]
       │
       ▼
[Usuário responde e aprova as decisões]
       │
       ▼
[Plano é gerado com todas as decisões registradas]
       │
       ▼
[Usuário aprova o plano → /start-work]
```

> **Nota**: Decisões concretas de design system (cores, dark mode, tipografia, ícones) serão definidas por tarefa, sob demanda, seguindo este mesmo fluxo.

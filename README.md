# Notify

**Plataforma de newsletter e notificacao por email com arquitetura hexagonal, mensageria assincrona e deploy em containers**

[![Java 17](https://img.shields.io/badge/Java-17-%23ED8B00?logo=java)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 4.0.6](https://img.shields.io/badge/Spring_Boot-4.0.6-%236DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular 22](https://img.shields.io/badge/Angular-22-%23DD0031?logo=angular)](https://angular.dev/)
[![TypeScript 6.0](https://img.shields.io/badge/TypeScript-6.0-%233178C6?logo=typescript)](https://www.typescriptlang.org/)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-%234169E1?logo=postgresql)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-4-%23FF6600?logo=rabbitmq)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-%232496ED?logo=docker)](https://www.docker.com/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-2.0.5-%2300C7B7)](https://testcontainers.com/)
[![Flyway](https://img.shields.io/badge/Flyway-11-%23CC0200?logo=flyway)](https://flywaydb.org/)
[![JWT](https://img.shields.io/badge/JWT-JJWT_0.12-%23000000?logo=jsonwebtokens)](https://github.com/jwtk/jjwt)
[![CI](https://img.shields.io/badge/CI-GitHub_Actions-%232088FF?logo=githubactions)](https://github.com/features/actions)

---

## Indice

- [Descricao do Projeto](#descricao-do-projeto)
- [Status do Projeto](#status-do-projeto)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Fluxo da Aplicacao](#fluxo-da-aplicacao)
- [Arquitetura](#arquitetura)
- [Decisoes de Arquitetura e Tecnologia](#decisoes-de-arquitetura-e-tecnologia)
- [Como Rodar o Projeto](#como-rodar-o-projeto)

---

## Descricao do Projeto

Notify e uma plataforma completa para gerenciamento de newsletters e notificacoes por email. Ela permite que **criadores de conteudo** publiquem campanhas de email e que **assinantes** se inscrevam para recebe-las.

O sistema resolve o problema de comunicacao escalavel entre criadores de conteudo e seu publico, oferecendo:

- **Cadastro e autenticacao** de usuarios com JWT
- **Gerenciamento de newsletters** com perfil publico para inscricao
- **Criacao e gerenciamento de campanhas** com ciclo de vida (rascunho -> pendente -> publicado -> enviado)
- **Envio assincrono de emails** via fila RabbitMQ com worker dedicado
- **Interface web responsiva** em Angular para criadores e assinantes
- **Multiplos ambientes** (desenvolvimento, teste, producao) com Docker Compose

---

## Status do Projeto

![Status](https://img.shields.io/badge/Status-Em_Desenvolvimento-yellow)

O projeto esta ativamente em desenvolvimento. As funcionalidades principais implementadas incluem:

| Funcionalidade | Status |
|---|---|
| Autenticacao e registro de usuarios | Concluido |
| Gerenciamento de newsletters | Concluido |
| Inscricao de assinantes | Concluido |
| Gerenciamento de assinantes (senders) | Concluido |
| Gerenciamento de campanhas (CRUD + status) | Concluido |
| Envio de email via fila assincrona | Concluido |
| Worker de email com retry e DLQ | Concluido |
| CI/CD com smoke tests | Concluido |
| Deploy Docker completo (6 servicos) | Concluido |
| Testes automatizados (206+ testes) | Concluido |

---

## Tecnologias Utilizadas

### Backend (API)

| Tecnologia | Versao | Proposito |
|---|---|---|
| Java | 17 | Linguagem principal |
| Spring Boot | 4.0.6 | Framework web e injecao de dependencias |
| Spring Data JPA | 4.0.6 | Persistencia e repositories |
| Spring Security | 4.0.6 | Autenticacao e autorizacao |
| Spring AMQP / RabbitMQ | 4.0.6 | Mensageria assincrona |
| Spring Mail | 4.0.6 | Envio de emails SMTP |
| Flyway | 11 | Migracoes de banco de dados |
| PostgreSQL | 16 | Banco de dados relacional |
| JJWT | 0.12.6 | Tokens JWT |
| Lombok | -- | Reducao de boilerplate |
| Testcontainers | 2.0.5 | Testes de integracao com containers reais |

### Worker (Envio Assincrono de Email)

| Tecnologia | Versao | Proposito |
|---|---|---|
| Java | 17 | Linguagem principal |
| Spring Boot | 4.0.6 | Framework base (sem HTTP) |
| Spring AMQP / RabbitMQ | 4.0.6 | Consumo de filas |
| Spring Mail | 4.0.6 | Envio SMTP |
| Testcontainers | 2.0.5 | Testes com RabbitMQ real |

### Frontend (SPA)

| Tecnologia | Versao | Proposito |
|---|---|---|
| Angular | 22 | Framework SPA |
| TypeScript | 6.0 | Linguagem |
| Angular Material | 22 | Componentes de UI |
| Angular CDK | 22 | Component toolkit |
| Vitest | 4 | Test runner |
| jsdom | 28 | DOM simulada para testes |
| ESLint | 10 | Analise estatica |
| Prettier | 3 | Formatacao de codigo |
| RxJS | 7.8 | Programacao reativa |

### Infraestrutura

| Tecnologia | Versao | Proposito |
|---|---|---|
| Docker Compose | -- | Orquestracao de containers |
| Nginx | Alpine | Gateway reverso e servidor estatico |
| RabbitMQ | 4 (Management) | Broker de mensagens |
| Mailpit | latest | SMTP falso para desenvolvimento |
| GitHub Actions | -- | CI/CD |

---

## Fluxo da Aplicacao

O sistema implementa 6 fluxos principais que descrevem o percurso completo dos dados:

### Fluxo 1 -- Registro de Usuario

```
Usuario -> Pagina de Registro -> Validacao de Dados -> Criacao da Conta -> Confirmacao
```

O usuario preenche nome, email e senha. O sistema valida os dados, cria a conta e permite o login em seguida.

### Fluxo 2 -- Autenticacao

```
Usuario -> Login (email + senha) -> Validacao de Credenciais -> Geracao JWT -> Dashboard
```

A autenticacao e baseada em JWT (access token de 15min + refresh token de 7 dias). O token e extraido do header `Authorization: Bearer <token>` e verificado em cada requisicao protegida.

### Fluxo 3 -- Inscricao em Newsletter

```
Visitante -> Perfil Publico do Sender -> Visualiza Newsletter -> Clica "Inscrever-se"
  -> Sistema Registra Inscricao -> Status "Active" -> Assinante Adicionado a Lista
```

Qualquer usuario pode se inscrever em newsletters publicas. A inscricao e imediata com status "Active".

### Fluxo 4 -- Gerenciamento de Assinantes (Sender)

```
Sender -> Area "Assinantes" -> GET /api/v1/newsletter/{slug}/subscribers
  -> Sistema Retorna: total, lista de emails, datas, status
```

Disponivel apenas para o dono da newsletter. A verificacao de ownership e feita extraindo o `userId` do JWT e comparando com o `ownerId` da newsletter.

### Fluxo 5 -- Criacao de Campanha

```
Sender -> Cria Campanha (DRAFT) -> Edita -> Submete (PENDING) -> Publica (PUBLISHED) -> Envia (SENT)
```

**Ciclo de vida da campanha:**

```
DRAFT ----> PENDING ----> PUBLISHED ----> SENT
  |           |
  +-- edit / delete (DRAFT & PENDING only)
```

| Status | Editavel? | Excluivel? | Publicavel? |
|---|---|---|---|
| DRAFT | Sim | Sim | Sim (-> PENDING ou -> PUBLISHED) |
| PENDING | Sim | Sim | Sim (-> PUBLISHED) |
| PUBLISHED | Nao | Nao | Nao |
| SENT | Nao | Nao | Nao |

### Fluxo 6 -- Envio de Newsletter

```
Sender Publica Campanha -> Sistema Consulta Assinantes Ativos
  -> Backend Publica Mensagem no RabbitMQ
    -> Worker Consome a Fila -> Envia Email via SMTP -> Registra Resultado
```

O fluxo de envio e assincrono e resiliente:

```
Backend -> newsletter.direct -> newsletter.subscription.confirmation.queue
                                    |
                                    v
                          ConfirmationEmailListener
                                    |
                                    v
                          ConsumeEmailService
                                    |
                                    v
                          EmailSendingService (dominio)
                                    |
                                    v
                          SmtpEmailSender (SMTP)
                                    |
                              +-----+-----+
                              v           v
                           Sucesso     Falha
                                         |
                                    +----+----+
                                    v         v
                                Retry 3x     DLQ
                            (1s, 3s, 9s)
```

O worker realiza ate 3 tentativas de envio com backoff geometrico (multiplicador 3). Apos a 3 falha, a mensagem e encaminhada para a Dead Letter Queue (DLQ) para investigacao manual.

---

## Arquitetura

### Visao Geral (Deploy -- 6 Containers)

```
                    +----------+
                    |  Usuario  |
                    +-----+----+
                          | :80
                          v
              +-----------------------+
              |  Nginx (Gateway)      |
              |  - Proxy reverso      |
              |  - Static Assets      |
              |  - SPA Fallback       |
              |  - Rate Limit         |
              +--+----------------+---+
           /api/v1/*         /*.js|.css|...
                 |                  |
                 v                  v
        +----------------+  +----------+
        |   Backend      |  | Frontend |
        |  Spring Boot   |  |  Angular |
        |   :8080        |  |  (Nginx) |
        +---+--------+---+  +----------+
            |        |
            v        v
     +-----------+  +-------------+
     | PostgreSQL |  |  RabbitMQ   |
     |    :5432   |  |   :5672     |
     +-----------+  +------+------+
                           |
                           v
                  +------------------+
                  |   Worker         |
                  | Spring Boot      |
                  |  (Sem HTTP)      |
                  +--------+---------+
                           |
                           v
                    +------------+
                    |  SMTP /    |
                    | Mailpit    |
                    +------------+
```

### Arquitetura Hexagonal (Ports & Adapters) -- Backend

```
+-----------------------------------------------------+
|          Interfaces (REST/WS)                       |
|  Controllers Spring -- finos, dependem de            |
|  interfaces de porta (AuthUseCase, etc.)              |
|  [Inbound Adapters]                                  |
+-----------------------------------------------------+
|          Application (Use Cases)                    |
|  Services de aplicacao -- orquestram dominio,        |
|  gerenciam transacoes, publicam eventos              |
|  [Application Services]                              |
+-----------------------------------------------------+
|          Domain (Core)                              |
|  Entidades puras -- sem JPA, sem frameworks          |
|  Value Objects, Aggregates, Domain Events            |
|  [Regras de negocio imutaveis]                       |
+-----------------------------------------------------+
|      Infrastructure (Adapters)                      |
|  JPA Entities + Repositories + Mappers               |
|  RabbitMQ, SMTP, Config                              |
|  [Outbound Adapters]                                 |
+-----------------------------------------------------+
```

### Estrutura de Diretorios

```
notify/
├── backend/                          # API Spring Boot (Java 17)
│   ├── src/main/java/com/notify/
│   │   ├── shared/                   # Cross-cutting: exceptions, utils, annotations
│   │   ├── identity/                 # Bounded Context: Autenticacao e Usuarios
│   │   │   ├── domain/               #   Entidades, VOs, Repository interfaces
│   │   │   │   ├── model/
│   │   │   │   └── repository/
│   │   │   ├── application/          #   Use cases e DTOs
│   │   │   │   ├── dto/
│   │   │   │   ├── port/
│   │   │   │   └── service/
│   │   │   ├── infrastructure/       #   JPA, mappers, config
│   │   │   │   ├── config/
│   │   │   │   └── repository/
│   │   │   └── interfaces/rest/      #   Controllers REST
│   │   ├── newsletter/               # Bounded Context: Newsletters e Campanhas
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   ├── infrastructure/
│   │   │   │   ├── config/
│   │   │   │   ├── messaging/
│   │   │   │   └── repository/
│   │   │   └── interfaces/rest/
│   │   └── NotifyApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml           # Config compartilhada
│   │   ├── application-dev.yml       # Config desenvolvimento
│   │   ├── application-prod.yml      # Config producao
│   │   └── db/migration/             # Flyway migrations (V1 a V9)
│   └── pom.xml
│
├── frontend/                         # SPA Angular 22
│   ├── src/app/
│   │   ├── core/                     # Servicos singleton, guards, interceptors
│   │   │   ├── services/             #   ApiService, AuthService, ThemeService
│   │   │   ├── interceptors/         #   Auth, error interceptors
│   │   │   └── guards/               #   Auth guard
│   │   ├── shared/                   # Componentes reutilizaveis
│   │   │   ├── components/           #   ThemeToggle
│   │   │   ├── directives/
│   │   │   ├── models/               #   Tipos compartilhados
│   │   │   └── pipes/
│   │   ├── features/                 # Modulos de funcionalidade
│   │   │   ├── auth/                 #   Login, registro
│   │   │   ├── newsletter/           #   Perfil publico da newsletter
│   │   │   └── sender/               #   Dashboard, assinantes, campanhas
│   │   ├── layouts/                  # Layouts (MainLayout)
│   │   └── app.routes.ts
│   ├── angular.json
│   ├── vitest.config.ts
│   └── package.json
│
├── worker/                           # Worker de email assincrono (Spring Boot)
│   ├── src/main/java/com/notify/worker/
│   │   ├── email/                    # Bounded Context: Envio de Email
│   │   │   ├── domain/               #   EmailMessage, EmailSendingService
│   │   │   ├── application/          #   ConsumeEmailService
│   │   │   ├── infrastructure/       #   SmtpEmailSender, RabbitMQ config
│   │   │   └── interfaces/           #   ConfirmationEmailListener
│   │   └── shared/                   #   Utilitarios compartilhados
│   └── pom.xml
│
├── deploy/                           # Deploy completo
│   ├── docker-compose.yml            # Orquestracao (6 servicos)
│   ├── .env                          # Variaveis do Compose
│   ├── backend/Dockerfile            # Multi-stage: JDK -> JRE
│   ├── frontend/Dockerfile           # Multi-stage: Node -> Nginx
│   ├── worker/Dockerfile             # Multi-stage: JDK -> JRE
│   ├── nginx/
│   │   ├── nginx.conf                # Gzip, timeouts, default_server 444
│   │   └── conf.d/
│   │       ├── gateway.conf          # Proxy /api/v1/, static cache, SPA
│   │       └── security.conf         # Rate limiting, server_tokens off
│   └── smoke-test.sh                 # 12 testes de fumaca
│
└── .github/workflows/ci.yml          # CI: format -> lint -> test -> build -> docker
```

### Modelo de Dados

```
users ----1:N----> user_roles <----N:1---- roles
  |
  |
  +---1:N----> newsletters (owner_id)
  |                |
  |                +---1:N----> subscriptions (subscriber_id)
  |                |
  |                +---1:N----> campaigns
  |
  +---1:N----> subscriptions (subscriber_id)
```

**Migracoes Flyway aplicadas:**

| Versao | Descricao |
|---|---|
| V1 | Tabelas de identidade (users, roles, user_roles, role_permissions) + seed |
| V2 | Token version em users |
| V3 | Tabela de subscriptions |
| V4 | Tabela de newsletters |
| V5 | Seed de newsletter do admin |
| V6 | subscriber_id em subscriptions |
| V7 | Tabela de campaigns |
| V8 | Seed de test user |
| V9 | Seed adicional de test users |

---

## Decisoes de Arquitetura e Tecnologia

### Por que Arquitetura Hexagonal (Ports & Adapters)?

A escolha pela arquitetura hexagonal foi motivada pela necessidade de isolar o dominio de frameworks externos. As regras de negocio -- como o ciclo de vida de campanhas (DRAFT -> PENDING -> PUBLISHED -> SENT) e a validacao de emails -- sao implementadas em `domain/` sem qualquer dependencia de Spring, JPA ou RabbitMQ. Isso significa que:

- **Dominio testavel isoladamente**: as regras de negocio sao testadas com JUnit puro, sem carregar contexto Spring.
- **Framework e detalhe de implementacao**: trocar JPA por JDBC, ou RabbitMQ por Kafka, nao afeta o dominio.
- **Portas explicitas**: todo ponto de integracao e uma interface (porta), facilitando mocks em testes e substituicoes em producao.

### Por que dois modulos separados (backend + worker)?

O worker de email e uma aplicacao Spring Boot independente, sem servidor HTTP, por tres razoes:

1. **Escalabilidade independente**: em cenarios de alto volume, o worker pode ser escalado horizontalmente sem afetar a API.
2. **Isolamento de falhas**: se o SMTP falhar, a API continua respondendo -- apenas o worker e impactado.
3. **Responsabilidade unica**: a API gerencia recursos (usuarios, newsletters, campanhas); o worker apenas envia emails. Cada um tem seu proprio ciclo de vida, deploy e monitoramento.

### Por que RabbitMQ em vez de Kafka ou Redis Pub/Sub?

RabbitMQ foi escolhido por:

- **Roteamento flexivel**: exchanges e routing keys permitem rotear mensagens para filas especificas (ex: confirmacoes de inscricao vs. envio de campanhas).
- **DLQ nativa**: Dead Letter Queue e um conceito de primeira classe no RabbitMQ, essencial para o fluxo de retry com backoff.
- **Simplicidade operacional**: para o volume esperado (milhares, nao milhoes de mensagens/dia), RabbitMQ oferece o ponto ideal entre funcionalidade e complexidade.
- **Spring AMQP com excelente integracao**: `RabbitTemplate` e `@RabbitListener` tornam a integracao trivial.

### Por que Flyway em vez de Liquibase?

Flyway foi escolhido por sua simplicidade e abordagem opinativa:

- Migracoes em SQL puro (nao XML/JSON) -- qualquer pessoa que conhece PostgreSQL entende as migracoes.
- Versionamento sequencial simples (V1, V2, V3...).
- Validacao de checksum evita que migracoes aplicadas sejam modificadas.
- `flyway:repair` para recuperacao de cenarios inconsistentes.

### Por que JWT em vez de sessao?

- **Stateless**: a API nao mantem estado de sessao no servidor, facilitando escalabilidade horizontal.
- **Desacoplamento**: frontend e backend podem estar em dominios diferentes sem necessidade de cookie compartilhado.
- **Tokens com escopo**: access token (15min) + refresh token (7d) com possibilidade de revogacao via `tokenVersion` no banco.

### Por que Angular 22 com Signals?

Angular 22 foi escolhido por ser a versao mais recente do framework, com suporte nativo a:

- **Signals**: sistema reativo moderno que substitui RxJS para estado local de componentes, com `signal()` e `computed()`.
- **Standalone components**: sem NgModules, reduzindo boilerplate e melhorando tree-shaking.
- **Vitest como test runner padrao**: mais rapido que Karma/Jasmine, com melhor integracao com TypeScript.
- **Angular Material 22**: componentes de UI prontos e acessiveis.

### Por que Testcontainers em vez de H2 em testes?

- **Fidelidade ao ambiente de producao**: H2 nao se comporta como PostgreSQL em consultas nativas, tipos (UUID, JSONB) e funcoes especificas.
- **Zero configuracao**: Testcontainers sobe um PostgreSQL temporario via Docker automaticamente -- sem necessidade de banco local.
- **Mesmo para testes de controller**: garante que o SQL gerado pelo Hibernate funciona no banco real.

### Por que Nginx como gateway unico?

- **Porta unica (:80)**: simplifica o deploy -- apenas uma porta exposta ao host.
- **Proxy reverso**: `/api/v1/*` e roteado para o backend; `/mailpit/*` para o Mailpit.
- **Static assets**: arquivos `.js`, `.css`, etc. servidos com `Cache-Control: public, immutable, max-age=31536000`.
- **SPA fallback**: qualquer rota nao correspondida retorna `index.html`.
- **Seguranca**: `return 444` para hosts desconhecidos, rate limiting (10 req/s), headers de seguranca, `server_tokens off`.
- **Resolucao DNS interna**: `resolver 127.0.0.11` permite que o Nginx resolva nomes de servico Docker em runtime.

### Por que a politica de Zero Trust?

A comunicacao segue o principio **"nunca confie, sempre verifique"**:

- **Cada camada valida**: controller (Jakarta Validation) -> application (regras de negocio + ownership) -> domain (invariantes do agregado).
- **IDs do corpo da requisicao nunca sao confiaveis**: o `userId` e extraido do JWT (`@AuthenticationPrincipal`), nao do corpo.
- **Mensagens da fila sao revalidadas**: o worker nao assume que a mensagem e valida so porque veio do backend -- valida novamente.
- **Frontend valida para UX, nao para seguranca**: toda validacao critica e refeita no servidor.

### Por que TDD (Test-Driven Development)?

Todo codigo e escrito seguindo TDD:

- **149 testes no backend**: dominio (puro JUnit), aplicacao (Spring integrado), controllers (RANDOM_PORT), worker (Testcontainers).
- **57 testes no frontend**: componentes com `TestBed`, servicos com `HttpTestingController`.
- **Testes de fumaca**: 12 testes em `deploy/smoke-test.sh` validam o deploy completo.
- **Cobertura minima**: 80% linhas, 70% branches -- verificada em CI.

---

## Como Rodar o Projeto

### Pre-requisitos

- [Docker](https://docs.docker.com/engine/install/) e [Docker Compose](https://docs.docker.com/compose/install/)
- Java 17+ (apenas para desenvolvimento local)
- Node.js 22+ e npm 11+ (apenas para desenvolvimento local frontend)
- Maven (opcional -- o wrapper `./mvnw` esta incluido)

### Desenvolvimento Local

#### 1. Suba os servicos de infraestrutura

```bash
cd backend
docker compose up -d
```

Isso inicia:
- **PostgreSQL 16** na porta `5432` (banco `notify`, usuario `notify`, senha `notify`)
- **RabbitMQ 4** na porta `5672` (management UI em `http://localhost:15672`, usuario `notify`, senha `notify`)
- **Mailpit** na porta `1025` (SMTP) e `8025` (UI para visualizar emails)

#### 2. Inicie o backend

```bash
cd backend
./mvnw spring-boot:run
```

A API estara disponivel em `http://localhost:8080`. O perfil `dev` e ativado automaticamente. Flyway executa as migracoes automaticamente na inicializacao.

#### 3. Inicie o frontend

```bash
cd frontend
npm install
npm start
```

A SPA estara disponivel em `http://localhost:4200`. Por padrao, redireciona para `/auth/login`.

#### 4. Credenciais padrao (seed data)

| Email | Senha | Role |
|---|---|---|
| `admin@notify.com` | `Admin@123` | ADMIN |
| `test@notify.com` | `Test@123` | USER |

### Producao (Docker Full Stack)

```bash
# Build e inicie todos os 6 servicos
docker compose -f deploy/docker-compose.yml up --build -d

# Verifique o status
docker compose -f deploy/docker-compose.yml ps

# Acompanhe os logs
docker compose -f deploy/docker-compose.yml logs -f

# Pare e limpe volumes (dados sao perdidos)
docker compose -f deploy/docker-compose.yml down -v
```

Acesse: `http://localhost:80`

O stack de producao inclui:

| Servico | Imagem | Portas | Depende de |
|---|---|---|---|
| `nginx` | `nginx:alpine` | `:80` -> `:80` | backend, frontend |
| `frontend` | `nginx:alpine` (static) | -- | -- |
| `backend` | `eclipse-temurin:17-jre-alpine` | -- | postgres, rabbitmq |
| `postgres` | `postgres:16` | -- | -- |
| `rabbitmq` | `rabbitmq:4-management-alpine` | -- | -- |
| `worker` | `eclipse-temurin:17-jre-alpine` | -- | rabbitmq |
| `mailpit` | `axllent/mailpit:latest` | -- | -- |

### Testes

#### Backend

```bash
cd backend
./mvnw clean test
```

Usa Testcontainers -- sobe PostgreSQL temporario via Docker automaticamente. Nao requer banco local.

#### Frontend

```bash
cd frontend
npm test
```

Usa Vitest com jsdom. Testes unitarios para componentes e servicos.

#### Smoke Tests (Deploy Completo)

```bash
bash deploy/smoke-test.sh http://localhost:80
```

Executa 12 testes de fumaca validando: SPA serving, API health, autenticacao, autorizacao, CRUD de campanhas, cache de assets, headers de seguranca e rate limiting.

### Verificacoes de Qualidade

#### Backend (Spotless + PMD + SpotBugs)

```bash
cd backend
./mvnw spotless:check       # Formatacao
./mvnw spotless:apply       # Auto-corrige formatacao
./mvnw pmd:check            # Analise estatica (best practices, error prone)
./mvnw spotbugs:check       # Deteccao de bugs (null safety, threading)
```

#### Frontend (Prettier + ESLint)

```bash
cd frontend
npm run format:check        # Verificacao de formatacao
npm run format              # Auto-correcao de formatacao
npm run lint                # Analise estatica ESLint
npm run quality             # Ambos (format + lint)
npm run quality:fix         # Auto-corrige ambos
```

### Variaveis de Ambiente

#### Backend (`application-prod.yml`)

| Variavel | Padrao | Descricao |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/notify` | URL do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `notify` | Usuario do banco |
| `SPRING_DATASOURCE_PASSWORD` | `notify` | Senha do banco |
| `JWT_SECRET` | (chave codificada em Base64) | Chave secreta para assinar JWTs |
| `JWT_ACCESS_EXPIRATION` | `900000` (15min) | Duracao do access token em ms |
| `JWT_REFRESH_EXPIRATION` | `604800000` (7 dias) | Duracao do refresh token em ms |
| `PASSWORD_PEPPER` | `change-me-in-production` | Pepper para hash de senha |
| `RABBITMQ_HOST` | `rabbitmq` | Host do RabbitMQ |
| `RABBITMQ_USERNAME` | `notify` | Usuario RabbitMQ |
| `RABBITMQ_PASSWORD` | `notify` | Senha RabbitMQ |

#### Worker (`application.yml`)

| Variavel | Padrao | Descricao |
|---|---|---|
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `RABBITMQ_USERNAME` | `notify` | Usuario RabbitMQ |
| `RABBITMQ_PASSWORD` | `notify` | Senha RabbitMQ |
| `MAIL_HOST` | `localhost` | Host SMTP |
| `MAIL_PORT` | `1025` | Porta SMTP |

#### Deploy (`.env`)

| Variavel | Padrao | Descricao |
|---|---|---|
| `COMPOSE_PROJECT_NAME` | `notify` | Nome do projeto Docker Compose |
| `API_URL` | (vazio) | URL da API para o frontend |
| `BACKEND_HOST` | `backend` | Hostname do backend (resolucao Docker DNS) |

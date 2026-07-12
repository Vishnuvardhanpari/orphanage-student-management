# Orphanage Management System (OMS)

Secure web application for managing student records within an orphanage. This repository contains the frontend (Angular), backend (Spring Boot), infrastructure configuration, and project documentation.

## Repository Structure

```text
orphanage-student-management/
├── backend/          # Spring Boot API (Milestone 1+)
├── frontend/         # Angular application (Milestone 1+)
├── database/         # Shared SQL seeds and database utilities
├── infra/            # Docker Compose and deployment helpers
├── docs/             # Project documentation
├── .github/          # CI/CD workflows (placeholder)
├── .env.example      # Environment variable template
└── README.md
```

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (for local PostgreSQL)
- JDK 21, Maven 3.9+ (backend)
- Node.js 20+ LTS (frontend; Node 24 verified)

## Before First Run

**Required before starting PostgreSQL or any application services:**

1. Copy `.env.example` to `.env` in the repository root.
2. Open `.env` and change `DB_PASSWORD` to a strong local password. Do not use the example value.
3. Never commit `.env` to Git — it is listed in `.gitignore`.

**Linux / macOS (bash):**

```bash
cp .env.example .env
```

**Windows (PowerShell):**

```powershell
Copy-Item .env.example .env
```

**Windows (Command Prompt):**

```cmd
copy .env.example .env
```

Docker Compose reads database credentials from `.env`. `DB_PASSWORD` is required — Compose will fail to start if it is empty.

## Local Development Setup

### 1. Environment variables

Complete the [Before First Run](#before-first-run) steps first, then confirm `.env` contains your chosen `DB_PASSWORD` and other values as needed.

### 2. Start PostgreSQL

From the repository root:

**Linux / macOS (bash):**

```bash
docker compose --env-file .env -f infra/docker-compose.yml up -d
```

**Windows (PowerShell):**

```powershell
docker compose --env-file .env -f infra/docker-compose.yml up -d
```

Verify the database is healthy:

```bash
docker compose --env-file .env -f infra/docker-compose.yml ps
```

### 3. Stop PostgreSQL

```bash
docker compose --env-file .env -f infra/docker-compose.yml down
```

To remove persisted data:

```bash
docker compose --env-file .env -f infra/docker-compose.yml down -v
```

### 4. Backend (Spring Boot)

**Prerequisites:** JDK 21 and Maven 3.9+. Ensure PostgreSQL is running (step 2).

Spring Boot reads database credentials from environment variables defined in the root `.env` file. Spring Boot does not load `.env` automatically — export the variables in your shell or configure your IDE run profile.

**Build and test:**

```powershell
cd backend
mvn clean verify
```

**Run locally (PowerShell — loads `.env` from repository root):**

```powershell
Get-Content ..\.env | ForEach-Object {
  if ($_ -match '^\s*([^#=]+?)=(.*)$') {
    Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim()
  }
}
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

**Verify:**

| Endpoint | Expected |
|----------|----------|
| `http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| `http://localhost:8080/actuator/prometheus` | Prometheus metrics text |
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI loads |

CORS (optional curl with Angular origin):

```powershell
curl.exe -i -H "Origin: http://localhost:4200" http://localhost:8080/actuator/health
```

Expect `Access-Control-Allow-Origin: http://localhost:4200` in the response headers.

**Docker image:**

```bash
docker build -t oms-backend backend/
```

**Note:** If host port `5432` is occupied, set `DB_PORT=5433` in `.env` (as verified in Phase A). Spring Boot must receive the same `DB_PORT` via exported environment variables.

### 5. Frontend (Angular)

**Prerequisites:** Node.js 20+ LTS.

```powershell
cd frontend
npm install
npm start
```

App: [http://localhost:4200](http://localhost:4200)

The dev server proxies `/api` to `http://localhost:8080` (Spring Boot). See [frontend/README.md](frontend/README.md) for build and test commands.

**Production build:**

```powershell
cd frontend
npm run build
```

### 6. Integration smoke (Phase D)

With PostgreSQL, backend, and frontend running:

1. Open `http://localhost:4200` — layout shell and routes load.
2. Open `http://localhost:8080/actuator/health` — status UP.
3. Open `http://localhost:8080/swagger-ui/index.html` — Swagger UI loads.
4. Proxy hop (expects Spring 404, not connection refused):

```powershell
curl.exe -i http://localhost:4200/api/v1/does-not-exist
```

## Documentation

Before implementing features, read the documentation in `docs/`:

1. [docs/12_AI_Context.md](docs/12_AI_Context.md)
2. [docs/Session_context.md](docs/Session_context.md)
3. [docs/13_DEVELOPMENT_ROADMAP.md](docs/13_DEVELOPMENT_ROADMAP.md)

## Current Milestone

**Milestone 1 — Project Initialization**

Phases A–D are complete (scaffold, Spring Boot, Angular, local integration). Next: Milestone 2 (Authentication) on branch `feature/authentication` after approval. See [infra/README.md](infra/README.md) for PostgreSQL setup and [frontend/README.md](frontend/README.md) for Angular.

## License

Private — orphanage management application.

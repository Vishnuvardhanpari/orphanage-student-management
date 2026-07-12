# Local Infrastructure

This folder contains Docker Compose configuration for local development.

## PostgreSQL

OMS uses PostgreSQL 16 locally via Docker Compose. Production uses Supabase PostgreSQL ([docs/09_Deployment.md](../docs/09_Deployment.md)).

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- Root `.env` created from `.env.example` with a strong `DB_PASSWORD`

### Start

From the repository root:

```bash
docker compose --env-file .env -f infra/docker-compose.yml up -d
```

### Verify

```bash
docker compose --env-file .env -f infra/docker-compose.yml ps
docker compose --env-file .env -f infra/docker-compose.yml exec postgres pg_isready -U oms_user -d oms_dev
```

Expected: container `oms-postgres` is `healthy`, and `pg_isready` reports accepting connections.

### Connection (for the Spring Boot app)

| Setting | Value |
|---------|--------|
| Host | `localhost` (`DB_HOST`) |
| Port | value of `DB_PORT` in `.env` (default `5432`) |
| Database | `oms_dev` (`DB_NAME`) |
| Username | `oms_user` (`DB_USERNAME`) |
| Password | value from `.env` (`DB_PASSWORD`) |
| JDBC URL | `jdbc:postgresql://localhost:<DB_PORT>/oms_dev` |

PostgreSQL is bound to `127.0.0.1` only so it is not reachable from other machines on the network.

**Port override:** If host port `5432` is already occupied (e.g. a Windows PostgreSQL service), set `DB_PORT=5433` in the root `.env`. Compose maps `127.0.0.1:5433→5432` inside the container. Spring Boot must use the same `DB_PORT` via exported environment variables.

### Stop

```bash
docker compose --env-file .env -f infra/docker-compose.yml down
```

Remove the data volume:

```bash
docker compose --env-file .env -f infra/docker-compose.yml down -v
```

### Notes

- Do not commit `.env`.
- Compose fails fast if `DB_PASSWORD` is unset (no silent weak default).
- Volume `oms_pg_data` persists data across container restarts (`down` without `-v`).
- Backend and frontend run on the host (not in this Compose file). Backend image: `docker build -t oms-backend backend/`.

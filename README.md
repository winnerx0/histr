# Histr

Spreadsheet-aware data application with a Spring Boot backend, a React dashboard, and pgvector-backed semantic search over uploaded `.xlsx` / `.csv` files.

## Stack

- **Backend** — Spring Boot 3.4 (Java 21), Spring Data JPA, Spring Data Redis, Spring AI (OpenAI), Apache POI, Commons CSV
- **Frontend** — React 19 + Vite 7 + TypeScript, TanStack Query
- **Data** — PostgreSQL 18 with `pgvector` 0.8, Redis 7
- **Edge** — Nginx + Certbot (Let's Encrypt) in production

## Layout

```
histr-api/        Spring Boot service (REST API, ingest, embeddings)
frontend/         React/Vite dashboard
nginx/            Nginx reverse-proxy config
cluster/          Cluster / infra manifests
init.sql          Postgres bootstrap (schema + pgvector extension)
docker-compose.yml          Local dev stack
docker-compose-prod.yml     Production stack (external DB, no certbot service)
```

## Local development

Requirements: Docker, Java 21, Maven, Bun (or npm).

```bash
# 1. Bring up Postgres + Redis + backend
export OPENAI_API_KEY=sk-...
docker compose up -d

# 2. Frontend
cd frontend
bun install
bun run dev
```

The backend listens on `:8080`, the frontend dev server on Vite's default `:5173`.

### Environment

| Variable | Purpose | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL for Postgres | `jdbc:postgresql://postgres:5432/histr` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | DB credentials | `postgres` / `password` |
| `REDIS_URL` | Redis connection | `redis://redis:6379` |
| `OPENAI_API_KEY` | OpenAI key for embeddings | — (required) |
| `OPENAI_EMBEDDING_DIMENSIONS` | Embedding vector size | `384` |
| `SERVER_PORT` | Backend HTTP port | `8080` |
| `UPLOAD_DIR` | Where uploads are persisted | `/app/uploads` |

## Production

`docker-compose-prod.yml` expects an externally managed Postgres (set `DATABASE_*` via `.env`) and runs Redis, the backend, and Nginx. TLS certs are mounted from the external `certbot_certs` Docker volume.

Deployment is automated by [.github/workflows/vps.yml](.github/workflows/vps.yml): on push to `main`, GitHub Actions builds the API, SSHes to the VPS, pulls the latest commit, rebuilds the compose stack, and restarts.

Required GitHub secrets: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, optional `VPS_APP_DIR` (defaults to `/opt/histr`).

## Build

```bash
# Backend
cd histr-api && mvn clean package -DskipTests

# Frontend
cd frontend && bun run build
```

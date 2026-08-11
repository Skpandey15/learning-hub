# Learning Hub V1 — Docker Compose Design

## 1. Goal

Provide one reproducible local command that starts the web application, API, AI service, LiteLLM gateway, PostgreSQL, and Keycloak without committing secrets.

## 2. Services

| Service | Container port | Host port | Dependency |
|---|---:|---:|---|
| `web` | 80 | 3000 | `api`, `keycloak` |
| `api` | 8080 | 8080 | `postgres`, `keycloak`, `ai-service` |
| `ai-service` | 8000 | 8000 | `litellm` |
| `litellm` | 4000 | 4000 | OpenAI network access |
| `postgres` | 5432 | 5432 | persistent volume |
| `keycloak` | 8080 | 8081 | `postgres` or dedicated local DB/schema |

For production, Keycloak should use an independently managed database. Local V1 may share the PostgreSQL server but must use a separate database and credentials.

## 3. Files

```text
platform/docker/
├── compose.yaml
├── .env.example
├── litellm-config.yaml
└── README.md
platform/keycloak/
└── learning-hub-realm.json
```

Each application owns a multi-stage Dockerfile and a `.dockerignore`.

## 4. Configuration

`.env.example` contains names and safe placeholders only:

```dotenv
POSTGRES_DB=learning_hub
POSTGRES_USER=learning_hub
POSTGRES_PASSWORD=change-me
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=change-me
KEYCLOAK_ISSUER=http://localhost:8081/realms/learning-hub
INTERNAL_AI_SERVICE_TOKEN=change-me
OPENAI_API_KEY=replace-locally
STUDY_MODEL=openai/your-approved-model
```

The real `.env` is gitignored. Production secrets come from the deployment secret store, not Compose environment files.

## 5. Networking

Only `web`, `api`, Keycloak, and development health ports need host exposure. FastAPI and LiteLLM should be internal-only in a hardened profile. Service-to-service URLs use Compose DNS names, never `localhost`.

Suggested networks:

- `edge`: web, API, Keycloak
- `backend`: API, FastAPI, LiteLLM, PostgreSQL, Keycloak

PostgreSQL data uses a named volume. Application containers use read-only root filesystems where practical and run as non-root users.

## 6. Startup and health

Health checks:

- PostgreSQL: `pg_isready`
- Keycloak: realm endpoint/health endpoint
- LiteLLM: health endpoint
- FastAPI: `/health/ready`
- Spring Boot: `/actuator/health/readiness`
- Web: static server health path

Use `depends_on` with health conditions for local sequencing, while applications also retry unavailable dependencies with bounded backoff. Flyway runs during Spring Boot startup or as a one-shot migration service; choose one approach and test it consistently. The default design uses Spring Boot startup migrations.

## 7. Keycloak realm

The imported realm defines:

- Public SPA client using authorization code + PKCE
- API audience/client scope
- Roles `candidate`, `interviewer`, and `admin`
- Two optional documented development users with passwords injected or changed locally
- Exact redirect URIs and web origins for `http://localhost:3000`

Never export real users, secrets, or production realm keys.

## 8. Local commands

```bash
cd platform/docker
docker compose config
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 api ai-service
```

Validation URLs:

- Web: `http://localhost:3000/learn`
- API readiness: `http://localhost:8080/actuator/health/readiness`
- AI readiness: `http://localhost:8000/health/ready`
- Keycloak: `http://localhost:8081`

## 9. Persistence and reset

Normal restarts retain PostgreSQL data. Documentation may provide an explicit development-only reset command, prominently warning that it removes local progress and generated lessons. Automated setup must never delete volumes implicitly.

## 10. Production differences

Compose validates integration but is not the production architecture. Production requires managed secrets, TLS/mTLS, backups, database high availability, centralized logs/metrics, default-deny internal networking and egress, image signing/scanning, resource limits, an identity-aware proxy, and a documented restore procedure. Follow `ZERO_TRUST_SECURITY.md`.

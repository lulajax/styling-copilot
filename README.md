# Smart Styling Copilot

Implementation based on `智能服装搭配系统规格书.md`.

## Scope implemented

### Backend (`Spring Boot 3.2`, Java 21)

- Authentication
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - JWT auth filter + stateless security
- Member management
  - `GET /api/members`
  - `POST /api/members`
  - `GET /api/members/{memberId}`
  - `PUT /api/members/{memberId}`
  - `DELETE /api/members/{memberId}` (soft delete)
  - `photoUrl` supported in create/update/query
- Clothing management
  - `GET /api/clothing?status=ON_SHELF&page&size`
  - `GET /api/clothing/all?page&size`
  - `POST /api/clothing`
  - `GET /api/clothing/{clothingId}`
  - `PUT /api/clothing/{clothingId}`
  - `PATCH /api/clothing/{clothingId}/status`
  - `DELETE /api/clothing/{clothingId}` (soft delete)
  - Clothing type field: `clothingType` (write accepts `TOP`/`BOTTOM`)
- File upload
  - `POST /api/files/upload?bizType=member|clothing` (`multipart/form-data`, field: `file`)
  - OSS-backed upload with file type/size validation (`jpg/jpeg/png/webp`, `<= 5MB`)
- Match task module
  - `POST /api/match/tasks`
  - `GET /api/match/tasks`
  - `GET /api/match/tasks/{taskId}`
  - `GET /api/match/tasks/{taskId}/events` (SSE)
  - `GET /api/members/{memberId}/history`
  - Persistent `match_task` task state
  - 7-day dedup (SQL + code-level double check)
  - pure AI strategy (`AI_ONLY`) for both cold and warm start
  - multi-outfit output: up to 3 `TOP + BOTTOM` outfits per task
  - when fewer than 3 valid outfits are available, task still succeeds with warning
  - per-outfit preview generation with per-outfit degradation warning
  - async processing + SSE events (`task_started`, `task_progress`, `task_completed`, `task_failed`)
  - per-operator task creation rate limit (`429`)
  - member profile sizes are provided to AI context, final selection is AI-driven

### Frontend (`Vue 3 + Vite + Element Plus`)

- Login and auth guard
- Dashboard
- Member management page
- Clothing management page
- Member/Clothing create and edit dialogs with image upload
- Structured member body form (UI fields -> JSON in API payload)
- Shared style tag multi-select options (10 fixed tags)
- Match Studio
  - member and clothing selection
  - create task
  - SSE real-time progress and final result
- Task Center
- History Review page
- Axios interceptor with refresh-token retry

## Run backend

Requires Java 21.

```bash
cd backend
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-open"
export PATH="$JAVA_HOME/bin:$PATH"
mvn spring-boot:run
```

Backend: `http://localhost:8080`

Default account:

- username: `stylist`
- password: `stylist123`

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

## Verification

Backend tests:

```bash
cd backend
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-open"
export PATH="$JAVA_HOME/bin:$PATH"
mvn test
```

Frontend build:

```bash
cd frontend
npm run build
```

## OpenAPI docs

After backend starts, open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## AI provider switch

Configure provider in `backend/src/main/resources/application.yml`:

```yaml
app:
  ai:
    enabled: true
    provider: gemini # gemini | openai
    gemini:
      base-url: https://generativelanguage.googleapis.com
      model: gemini-2.5-flash
      api-key: ${GEMINI_API_KEY:}
      connect-timeout-ms: 2000
      read-timeout-ms: 8000
      proxy:
        host: ${GEMINI_PROXY_HOST:}
        port: ${GEMINI_PROXY_PORT:0}
    openai:
      base-url: https://api.openai.com
      model: gpt-4o-mini
      api-key: ${OPENAI_API_KEY:}
      connect-timeout-ms: 2000
      read-timeout-ms: 8000
      proxy:
        host: ${OPENAI_PROXY_HOST:}
        port: ${OPENAI_PROXY_PORT:0}
```

AI calls are implemented through `langchain4j` for both OpenAI and Gemini providers.
When `app.ai.enabled=false` or the selected provider key is missing/invalid, task recommendation and preview generation will fail with `FAILED` status (pure AI mode).
When `proxy.host` is configured with `proxy.port > 0`, provider traffic is sent through the configured HTTP proxy; otherwise direct connection is used.

## Current limitations

- AI suggestions and preview generation require `app.ai.enabled=true` and a valid key for the selected provider; otherwise match tasks will fail in pure AI mode.
- Recommendation `reason` and preview text fields (`title/outfitDescription/imagePrompt`) follow request `Accept-Language` (`zh`/`en`/`ko`, fallback `en`).
- Preview is degraded (task still `SUCCEEDED` with `preview=null`) when member photo or any selected clothing image is missing.
- `app.ai.*.proxy.*` currently supports HTTP proxy without authentication (`host` + `port`).
- Persistence currently uses `ddl-auto=update` for local bootstrap. Production migration tooling (Flyway/Liquibase) is still pending.

# Testing Guide

## Backend

- Run lint: `cd backend && ./mvnw validate`
- Run tests: `cd backend && ./mvnw test`
- Generate API docs into `docs/`: `cd backend && ./mvnw -Popenapi verify`
- Checkstyle runs automatically during Maven validation.
- JaCoCo report is generated during `test` at `backend/target/site/jacoco/index.html`.
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Generated static files after `-Popenapi verify`:
  - `docs/openapi.json`
  - `docs/openapi.yaml`

Current coverage focus:
- `AuthService`
- `AuthController`
- `JwtAuthenticationFilter`
- `UserService`
- `UserController`
- `MeetingService`
- `MeetingController`
- `MeetingSystemController`
- `ChatController`
- `RecordingController`
- `LiveKitWebhookController`
- `GlobalExceptionHandler`

## Web Client

- Install dependencies: `cd web-client && npm install`
- Run lint: `npm run lint`
- Run tests once: `npm run test:run`
- Watch mode: `npm run test`

Current coverage focus:
- `AuthContext`
- `meetingService`
- `App` route guards
- `LoginPage`
- `SignUpPage`
- `ForgotPassword`
- `ResetPassword`
- `SchedulePage`
- `WaitingRoomPage`

## CI

GitHub Actions runs:
- Backend lint + unit tests
- Frontend lint + tests + build

## Git Hook

- Install the shared hook path once per clone:
  - PowerShell: `./scripts/install-git-hooks.ps1`
  - Bash: `./scripts/install-git-hooks.sh`
- The single `pre-commit` hook runs:
  - Backend `./mvnw -Popenapi verify`
  - Frontend `npm run lint`
  - Frontend `npm run test:run`
  - A diff check to ensure `docs/openapi.json` and `docs/openapi.yaml` are committed
- Run the same flow manually with: `./scripts/pre-commit-checks.sh`

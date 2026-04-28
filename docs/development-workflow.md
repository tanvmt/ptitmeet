# Development Workflow

## Goal

The team uses one shared `pre-commit` hook for the three required checks:

- Lint
- Test
- Swagger synchronization

GitHub Actions reruns only the checks that do not depend on local backend environment setup.

## Daily Flow

1. Install the repository hook after cloning.
2. Work normally.
3. Commit as usual.
4. Let the `pre-commit` hook block the commit if lint, tests, or Swagger docs are out of sync.

## Install The Hook

Use one of these commands once per clone:

```powershell
./scripts/install-git-hooks.ps1
```

```bash
./scripts/install-git-hooks.sh
```

This sets `core.hooksPath` to `.githooks` for the current clone.

## What The Hook Runs

The shared `pre-commit` hook executes:

```bash
cd backend && ./mvnw -Popenapi verify
cd web-client && npm run lint
cd web-client && npm run test:run
git diff --quiet -- docs/openapi.json docs/openapi.yaml
```

### Why `-Popenapi verify`

The backend now keeps OpenAPI generation inside the Maven profile `openapi`.

- `./mvnw verify` no longer starts the backend by default.
- `./mvnw -Popenapi verify` is the explicit command for generating Swagger files.
- This keeps normal Maven usage lighter and reduces accidental JVM crash dumps during development.

## CI Behavior

GitHub Actions now runs:

- Backend lint with `./mvnw validate`
- Backend tests with `./mvnw test`
- Frontend lint
- Frontend tests
- Frontend build

GitHub Actions does not generate Swagger docs right now because the backend startup path depends on local environment values.

## Swagger Sync Rule

If a backend API change affects the generated OpenAPI output, the commit must include:

- `docs/openapi.json`
- `docs/openapi.yaml`

If the hook says the docs are out of sync, rerun the backend OpenAPI command and stage the updated files.

## About `hs_err_pid*.log` And `replay_pid*.log`

Those files are JVM crash dumps, not normal application logs.

In this repository they were most likely created when Maven `verify` started the backend to generate Swagger docs and the Java process ran out of native memory. The files now have two protections:

- OpenAPI generation only runs when `-Popenapi` is explicitly enabled.
- JVM crash dump paths for that flow are redirected into `backend/target/`.

The repository also ignores:

- `hs_err_pid*`
- `replay_pid*`

If these files still appear often, check local RAM pressure, the Java processes opened by the IDE, and whether multiple backend instances are running at the same time.

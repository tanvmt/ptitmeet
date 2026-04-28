#!/bin/sh

set -eu

REPO_ROOT=$(git rev-parse --show-toplevel)

echo "Running backend verify with OpenAPI generation..."
(
  cd "$REPO_ROOT/backend"
  mvn -q -Popenapi spring-boot:stop >/dev/null 2>&1 || true
  ./mvnw -q -Popenapi verify
)

echo "Running frontend lint..."
(
  cd "$REPO_ROOT/web-client"
  npm run lint
)

echo "Running frontend tests..."
(
  cd "$REPO_ROOT/web-client"
  npm run test:run
)

echo "Checking generated OpenAPI files are committed..."
if ! git -C "$REPO_ROOT" diff --quiet -- docs/openapi.json docs/openapi.yaml; then
  echo "OpenAPI docs are out of sync. Regenerate them and commit docs/openapi.json plus docs/openapi.yaml." >&2
  exit 1
fi

echo "Pre-commit checks passed."

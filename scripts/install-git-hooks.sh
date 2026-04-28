#!/bin/sh

set -eu

REPO_ROOT=$(git rev-parse --show-toplevel)
git config core.hooksPath "$REPO_ROOT/.githooks"

echo "Git hooks installed. core.hooksPath -> $REPO_ROOT/.githooks"

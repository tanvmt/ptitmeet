$ErrorActionPreference = "Stop"

$repoRoot = git rev-parse --show-toplevel
git config core.hooksPath "$repoRoot/.githooks"

Write-Host "Git hooks installed. core.hooksPath -> $repoRoot/.githooks"

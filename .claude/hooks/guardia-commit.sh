#!/usr/bin/env bash
# PreToolUse (Bash): Git flow rules for commits. It does not run tests: the CI does that.
#  - no direct commits on main or develop
#  - conventional commit message when it is passed with -m
# Exit code 2 = block; the stderr message is returned to the assistant.
input=$(cat)

# Only "git commit" matters (not "git commit-tree", etc.).
printf '%s' "$input" | grep -qE 'git[[:space:]]+commit([[:space:]]|\\|"|$)' || exit 0

cd "$(dirname "$0")/../.." || exit 0

if git rev-parse --verify -q HEAD >/dev/null 2>&1; then
  branch=$(git symbolic-ref --short -q HEAD 2>/dev/null)
  if [ "$branch" = "main" ] || [ "$branch" = "develop" ]; then
    echo "BLOQUEADO: no se hace commit directo en '$branch'. Cree una rama desde develop (git switch develop && git switch -c feature/NNN-nombre) y haga el commit alli; la rama entra por Pull Request." >&2
    exit 2
  fi
fi

if printf '%s' "$input" | grep -qE '(^|[[:space:]])-[a-zA-Z]*m[[:space:]]'; then
  if ! printf '%s' "$input" | grep -qE '(feat|fix|docs|test|refactor|chore|ci|build|perf|revert)(\([a-z0-9._-]+\))?!?: '; then
    echo "BLOQUEADO: el mensaje de commit debe ser convencional: tipo(alcance): descripcion, con tipo en feat|fix|docs|test|refactor|chore|ci|build|perf|revert. Ejemplo: feat(franchise): agrega el endpoint de creacion." >&2
    exit 2
  fi
fi

exit 0

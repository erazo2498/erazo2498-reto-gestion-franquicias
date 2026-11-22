#!/usr/bin/env bash
# PreToolUse (Edit|Write): blocks what must never be left to the assistant's judgement:
# the real .env file and Terraform state / sensitive values files.
# Exit code 2 = block; the stderr message is returned to the assistant.
input=$(cat)
path=$(printf '%s' "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n1)
path=$(printf '%s' "$path" | tr '\\' '/' | tr -s '/')

[ -z "$path" ] && exit 0

name="${path##*/}"

if [[ "$name" == ".env" || ( "$name" == .env.* && "$name" != ".env.example" ) ]]; then
  echo "BLOQUEADO: $name contiene secretos y no se edita desde el asistente. Documente la variable nueva en .env.example; en AWS el valor vive en Secrets Manager." >&2
  exit 2
fi

if [[ "$name" =~ \.(tfstate|tfstate\.backup)$ || "$name" =~ \.auto\.tfvars$ ]]; then
  echo "BLOQUEADO: $name es estado de Terraform o un archivo de valores sensibles. El estado va en el backend remoto (S3); no se edita ni se versiona." >&2
  exit 2
fi

exit 0

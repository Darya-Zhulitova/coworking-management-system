#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env}"
BACKUP_DIR="${BACKUP_DIR:-$DEPLOY_DIR/backups}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Environment file not found: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

USER_DB_NAME="${USER_DB_NAME:-userservice}"
USER_DB_USERNAME="${USER_DB_USERNAME:-userservice}"
ADMIN_DB_NAME="${ADMIN_DB_NAME:-adminservice}"
ADMIN_DB_USERNAME="${ADMIN_DB_USERNAME:-adminservice}"

mkdir -p "$BACKUP_DIR"
cd "$DEPLOY_DIR"

backup_db() {
  local service="$1"
  local db_name="$2"
  local db_user="$3"
  local output="$BACKUP_DIR/${service}_${TIMESTAMP}.sql.gz"

  echo "Creating backup: $output"
  docker compose --env-file "$ENV_FILE" exec -T "$service" \
    pg_dump -U "$db_user" -d "$db_name" --clean --if-exists \
    | gzip -9 > "$output"
}

backup_db user-postgres "$USER_DB_NAME" "$USER_DB_USERNAME"
backup_db admin-postgres "$ADMIN_DB_NAME" "$ADMIN_DB_USERNAME"

find "$BACKUP_DIR" -type f -name '*.sql.gz' -mtime +14 -delete

echo "Backups are stored in $BACKUP_DIR"

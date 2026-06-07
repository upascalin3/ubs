#!/usr/bin/env bash
# Export PostgreSQL database backup for UBS (wasac_ms).
# Usage: ./scripts/export-db.sh [output-file]

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -f .env ]]; then
	set -a
	# shellcheck disable=SC1091
	source .env
	set +a
fi

POSTGRES_DB="${POSTGRES_DB:-wasac_ms}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"

BACKUP_DIR="${ROOT}/backups"
mkdir -p "$BACKUP_DIR"

if [[ -n "${1:-}" ]]; then
	BACKUP_FILE="$1"
	if [[ "$BACKUP_FILE" != /* ]]; then
		BACKUP_FILE="${ROOT}/${BACKUP_FILE}"
	fi
else
	TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
	BACKUP_FILE="${BACKUP_DIR}/${POSTGRES_DB}_${TIMESTAMP}.sql"
fi

export PGPASSWORD="$POSTGRES_PASSWORD"

echo "Exporting database '${POSTGRES_DB}' from ${POSTGRES_HOST}:${POSTGRES_PORT} ..."
pg_dump \
	-h "$POSTGRES_HOST" \
	-p "$POSTGRES_PORT" \
	-U "$POSTGRES_USER" \
	-d "$POSTGRES_DB" \
	--clean \
	--if-exists \
	--no-owner \
	--no-acl \
	--format=plain \
	--file="$BACKUP_FILE"

ln -sfn "$(basename "$BACKUP_FILE")" "${BACKUP_DIR}/latest.sql"

echo "Backup written to: ${BACKUP_FILE}"
echo "Latest symlink:    ${BACKUP_DIR}/latest.sql"

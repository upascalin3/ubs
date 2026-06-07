#!/usr/bin/env bash
# Import PostgreSQL database backup for UBS (wasac_ms).
# Usage: ./scripts/import-db.sh [backup-file]
# Default backup file: backups/latest.sql

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

if [[ -n "${1:-}" ]]; then
	BACKUP_FILE="$1"
	if [[ "$BACKUP_FILE" != /* ]]; then
		BACKUP_FILE="${ROOT}/${BACKUP_FILE}"
	fi
else
	BACKUP_FILE="${ROOT}/backups/latest.sql"
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
	echo "Backup file not found: ${BACKUP_FILE}" >&2
	exit 1
fi

export PGPASSWORD="$POSTGRES_PASSWORD"

echo "Ensuring database '${POSTGRES_DB}' exists ..."
DB_EXISTS="$(psql \
	-h "$POSTGRES_HOST" \
	-p "$POSTGRES_PORT" \
	-U "$POSTGRES_USER" \
	-d postgres \
	-tAc "SELECT 1 FROM pg_database WHERE datname = '${POSTGRES_DB}'")"

if [[ "$DB_EXISTS" != "1" ]]; then
	createdb \
		-h "$POSTGRES_HOST" \
		-p "$POSTGRES_PORT" \
		-U "$POSTGRES_USER" \
		"$POSTGRES_DB"
	echo "Created database '${POSTGRES_DB}'."
else
	echo "Database '${POSTGRES_DB}' already exists."
fi

echo "Importing backup from: ${BACKUP_FILE}"
psql \
	-h "$POSTGRES_HOST" \
	-p "$POSTGRES_PORT" \
	-U "$POSTGRES_USER" \
	-d "$POSTGRES_DB" \
	-v ON_ERROR_STOP=1 \
	-f "$BACKUP_FILE"

echo "Import completed successfully into '${POSTGRES_DB}'."

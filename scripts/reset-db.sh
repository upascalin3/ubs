#!/usr/bin/env bash
# Wipes all UBS data and Flyway history. Restart the app to re-run migrations + seed users.
set -euo pipefail

DB="${POSTGRES_DB:-wasac_ms}"
USER="${POSTGRES_USER:-postgres}"
export PGPASSWORD="${POSTGRES_PASSWORD:-postgres}"
HOST="${POSTGRES_HOST:-localhost}"
PORT="${POSTGRES_PORT:-5432}"

echo "Resetting database: $DB @ $HOST:$PORT"

psql -h "$HOST" -p "$PORT" -U "$USER" -d "$DB" -v ON_ERROR_STOP=1 <<'SQL'
DROP SCHEMA IF EXISTS audit CASCADE;
DROP SCHEMA IF EXISTS notification CASCADE;
DROP SCHEMA IF EXISTS payment CASCADE;
DROP SCHEMA IF EXISTS billing CASCADE;
DROP SCHEMA IF EXISTS meter CASCADE;
DROP SCHEMA IF EXISTS customer CASCADE;
DROP SCHEMA IF EXISTS auth CASCADE;
DROP TABLE IF EXISTS public.flyway_schema_history CASCADE;
SQL

echo "Done. All UBS schemas dropped."
echo "Start the app: ./run.sh  (Flyway V1–V10 + seed admin/operator/finance)"

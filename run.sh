#!/usr/bin/env bash
set -a
[ -f .env ] && source .env
set +a
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
exec ./mvnw spring-boot:run "$@"

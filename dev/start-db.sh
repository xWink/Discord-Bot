#!/bin/sh
# Starts a throwaway local MariaDB on port 33306 with the dev schema.
set -e
if ! docker info >/dev/null 2>&1; then
  echo "Starting Docker Desktop..."
  open -a Docker
  for i in $(seq 1 45); do docker info >/dev/null 2>&1 && break; sleep 2; done
  docker info >/dev/null 2>&1 || { echo "Docker did not start. Open Docker Desktop and retry."; exit 1; }
fi
docker rm -f gryph-dev-db >/dev/null 2>&1 || true
docker run -d --name gryph-dev-db -e MARIADB_ROOT_PASSWORD=dev -e MARIADB_DATABASE=discord_bot \
  -p 127.0.0.1:33306:3306 mariadb:10.11 >/dev/null
until docker exec gryph-dev-db mariadb -uroot -pdev -e 'select 1' >/dev/null 2>&1; do sleep 1; done
docker exec -i gryph-dev-db mariadb -uroot -pdev discord_bot < "$(dirname "$0")/schema.sql"
echo "ready: jdbc port 33306, user root, password dev"

#!/bin/sh
# Runs the bot against the local dev database. Usage: ./dev/run.sh
set -e
cd "$(dirname "$0")/.."
if [ ! -f .rolebotconfig ]; then
  echo "Missing .rolebotconfig. Copy .rolebotconfig.example to .rolebotconfig and fill it in (see dev/TESTING.md)."
  exit 1
fi
if grep -q "PASTE_YOUR_BOT_TOKEN_HERE" .rolebotconfig; then
  echo "Put your bot token in .rolebotconfig first."
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "Starting Docker Desktop..."
  open -a Docker
  for i in $(seq 1 45); do docker info >/dev/null 2>&1 && break; sleep 2; done
  docker info >/dev/null 2>&1 || { echo "Docker did not start. Open Docker Desktop and retry."; exit 1; }
fi
if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^gryph-dev-db$'; then
  echo "Starting the local database..."
  ./dev/start-db.sh
fi
exec sh gradlew run

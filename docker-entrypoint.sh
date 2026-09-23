#!/usr/bin/env bash
# FazziMart container entrypoint.
# Render injects a dynamic $PORT; defaults to 9090 for local docker run.
# DB settings come from the environment (DB_URL / DB_USER / DB_PASSWORD),
# which DBConnection.java reads before falling back to db.properties.
set -e

exec java -jar /app/app.jar --server.port="${PORT:-9090}"
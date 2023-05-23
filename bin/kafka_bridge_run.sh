#!/bin/sh
set -x

# Find my path to use when calling scripts
MYPATH="$(dirname "$0")"

# Make sure that we use /dev/urandom
JAVA_OPTS="${JAVA_OPTS} -Dvertx.cacheDirBase=/tmp/vertx-cache -Djava.security.egd=file:/dev/./urandom"

# enabling OpenTelemetry with OTLP by default
if [ -n "$OTEL_SERVICE_NAME" ] && [ -z "$OTEL_TRACES_EXPORTER" ]; then
  export OTEL_TRACES_EXPORTER="otlp"
fi

exec java $JAVA_OPTS -jar "${MYPATH}"/../quarkus-run.jar "$@"
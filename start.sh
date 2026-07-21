#!/bin/bash
cd "$(dirname "$0")"
echo "Starting battle simulation server..."
java -jar target/gekitotsu_java-1.3.4.jar --mode server "$@"

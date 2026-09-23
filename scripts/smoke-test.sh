#!/bin/bash
set -euo pipefail
if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 /path/to/jdk/Contents/Home [--gui]" >&2; exit 2
fi
project=$(cd "$(dirname "$0")/.." && pwd)
java_home=$(cd "$1" && pwd)
"$java_home/bin/java" -version
shift
"$java_home/bin/java" "$project/tests/LegacySmoke.java" "$@"

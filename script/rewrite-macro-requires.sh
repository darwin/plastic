#!/usr/bin/env bash
set -ex

if [ "$(uname)" != "Darwin" ]; then
  echo "Supported only under OS X."
  exit 1
fi

# ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

SRC_DIR="src"

COMMON_MACROS="defonce"
MAIN_MACROS="dispatch react! worker-dispatch"
WORKER_MACROS="dispatch react! main-dispatch"
LOGGING_MACROS="log info warn error group group-end"

ack -l '\[plastic\.common \:refer \[' "$SRC_DIR"  | xargs perl -pi -E "s/\[plastic\.common \:refer \[.*?\]/[plastic.common :refer [$COMMON_MACROS]/g"
ack -l '\[plastic\.main \:refer \[' "$SRC_DIR"    | xargs perl -pi -E "s/\[plastic\.main \:refer \[.*?\]/[plastic.main :refer [$MAIN_MACROS]/g"
ack -l '\[plastic\.worker \:refer \[' "$SRC_DIR"    | xargs perl -pi -E "s/\[plastic\.worker \:refer \[.*?\]/[plastic.worker :refer [$WORKER_MACROS]/g"
ack -l '\[plastic\.logging \:refer \[' "$SRC_DIR" | xargs perl -pi -E "s/\[plastic\.logging \:refer \[.*?\]/[plastic.logging :refer [$LOGGING_MACROS]/g"
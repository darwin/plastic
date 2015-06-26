#!/usr/bin/env bash
set -e

if [ "$(uname)" != "Darwin" ]; then
  echo "Supported only under OS X."
  exit 1
fi

# Ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

SRC_DIR="src"

COMMON_MACROS="defonce"
GLUE_MACROS="dispatch react!"
LOGGING_MACROS="log info warn error group group-end"

ack -l '\[quark\.macros.common \:refer \[' "$SRC_DIR"  | xargs perl -pi -E "s/\[quark\.macros.common \:refer \[.*?\]/[quark.macros.common :refer [$COMMON_MACROS]/g"
ack -l '\[quark\.macros.glue \:refer \[' "$SRC_DIR"    | xargs perl -pi -E "s/\[quark\.macros.glue \:refer \[.*?\]/[quark.macros.glue :refer [$GLUE_MACROS]/g"
ack -l '\[quark\.macros.logging \:refer \[' "$SRC_DIR" | xargs perl -pi -E "s/\[quark\.macros.logging \:refer \[.*?\]/[quark.macros.logging :refer [$LOGGING_MACROS]/g"

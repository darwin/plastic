#!/usr/bin/env bash
set -e

# ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

lein clean && lein figwheel $@
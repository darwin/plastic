#!/usr/bin/env bash
set -ex

# ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

lein clean && rlwrap lein figwheel dev devcards

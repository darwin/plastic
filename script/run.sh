#!/usr/bin/env bash
set -e

# ensure we start in cljs project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ../cljs

lein clean && lein figwheel main worker

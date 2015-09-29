#!/usr/bin/env bash
set -e

# ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

lein clean && lein cljsbuild once test
../node_modules/karma/bin/karma start ../karma/test.conf.js



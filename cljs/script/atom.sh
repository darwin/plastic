#!/usr/bin/env bash
set -e

# Ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

ROOT=`pwd`
SRC="$ROOT/src"
TMP="$ROOT/.tmp"
ATOM="$ROOT/.atom"

ATOM_HOME="$ATOM" ATOM_DEV_RESOURCE_PATH="$ROOT/../.." /Applications/Atom-dev.app/Contents/MacOS/Atom --log-file "$TMP/atom.log" $SRC

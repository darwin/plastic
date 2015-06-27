#!/usr/bin/env bash
set -e

# Ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

ROOT=`pwd`
SRC="$ROOT/src"
TMP="$ROOT/.tmp"
ATOM="$ROOT/.atom"

# atom is started in dev mode, if ROOT is ~/github/quark/cljs that means
#   ATOM_DEV_RESOURCE_PATH is ~/github/atom (checkout of atom repo)
#   ATOM_HOME is ~/github/quark/cljs/.atom
#
# you have to build atom from ~/github/atom, follow official build instructions
#
# running atom in --dev mode will enable dev-live-reload package, which will provide live reloading of css (less)

ATOM_HOME="$ATOM" ATOM_DEV_RESOURCE_PATH="$ROOT/../../atom" /Applications/Atom.app/Contents/MacOS/Atom --dev --log-file "$TMP/atom.log" $SRC

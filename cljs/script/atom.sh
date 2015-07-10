#!/usr/bin/env bash
set -e

# Ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

ROOT=`pwd`
SRC="$ROOT/src"

# this folder is plastic/cljs
WORKSPACE="$ROOT/../.." # workspace is parent folder of our root folder
ATOM_HOME="$WORKSPACE/.atom"
ATOM_DEV_RESOURCE_PATH="$WORKSPACE/atom"
TMP="$WORKSPACE/.tmp"

# atom is started in dev mode, if ROOT is ~/workspace/plastic/cljs that means
#   ATOM_DEV_RESOURCE_PATH is ~/workspace/atom (checkout of atom repo)
#   ATOM_HOME is ~/workspace/plastic/cljs/.atom
#
# you have to build atom from ~/workspace/atom, follow official build instructions
#
# running atom in --dev mode will enable dev-live-reload package, which will provide live reloading of css (less)

ATOM_HOME="$ATOM_HOME" ATOM_DEV_RESOURCE_PATH="$ATOM_DEV_RESOURCE_PATH" /Applications/Atom.app/Contents/MacOS/Atom --dev --log-file "$TMP/atom.log" $SRC

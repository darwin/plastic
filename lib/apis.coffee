{File, Directory} = require 'pathwatcher'
{$, $$, $$$} = require 'space-pen'
{TextEditor} = require 'atom'
info = require './info'

# select APIs we want to publish to ClojureScript
# see plastic.onion.apis

module.exports =
  atom: atom
  File: File
  Directory: Directory
  TextEditor: TextEditor
  $: $
  $$: $$
  $$$: $$$
  info: info

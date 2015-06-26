require("./boot")

path = require 'path'
QuarkEditorView = require './quark-editor-view'
{CompositeDisposable} = require 'atom'
bridge = require './bridge'

quarkEditorOpener = (uri) ->
  if path.extname(uri) is '.cljs'
    editorView = new QuarkEditorView({uri: uri})
    editorView

module.exports = Quark =
  subscriptions: null

  activate: (state) ->
    bridge.send "init"

    atom.workspace.addOpener quarkEditorOpener

    @subscriptions = new CompositeDisposable

    @subscriptions.add atom.commands.add 'atom-workspace', 'quark:toggle': => @toggle()
    @subscriptions.add atom.commands.add 'atom-workspace', 'quark:wake': => @wake()

  deactivate: ->
    @subscriptions.dispose()

  serialize: ->

  wake: ->
    console.info "Quark is here"

  toggle: ->
    console.log 'Quark!'

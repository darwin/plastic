require './boot'

path = require 'path'
QuarkEditorView = require './quark-editor-view'
{CompositeDisposable} = require 'atom'
bridge = require './bridge'
apis = require './apis'

quarkEditorOpener = (uri) ->
  if path.extname(uri) is '.cljs'
    editorView = new QuarkEditorView({uri: uri})
    editorView

module.exports = Quark =
  subscriptions: null

  activate: (state) ->
    bridge.send "apis", apis
    bridge.send "init", state # TODO: unserialize our part and pass rest

    atom.workspace.addOpener quarkEditorOpener

    @subscriptions = new CompositeDisposable

    @subscriptions.add atom.commands.add 'atom-workspace', 'quark:toggle': => @toggle()

  deactivate: ->
    @subscriptions.dispose()

  serialize: ->

  toggle: ->
    console.log 'Quark!'
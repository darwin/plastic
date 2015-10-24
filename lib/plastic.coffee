require './boot'

path = require 'path'
PlasticEditorView = require './plastic-editor-view'
{CompositeDisposable} = require 'atom'
bridge = require './bridge'
services = require './services'

plasticEditorOpener = (uri) ->
  if path.extname(uri) is '.cljs'
    editorView = new PlasticEditorView({uri: uri})
    editorView

module.exports = Plastic =
  subscriptions: null

  activate: (state) ->
    bootPlastic = =>
      goog.require("plastic.core")
      goog.require("plastic.api")
      goog.require("plastic.main")
      goog.require("plastic.worker") if plastic.config.run_worker_on_main_thread

      initAPIs = =>
        @system = bridge.boot plastic.config, services
        bridge.send "init", state # TODO: unserialize our part and pass rest

      setTimeout initAPIs, 100

    setTimeout bootPlastic, 1000 # TODO: this is temporary, give cljs-devtools some time to initialize

    atom.workspace.addOpener plasticEditorOpener

    @subscriptions = new CompositeDisposable

    @addCommands [
      'plastic:toggle-headers'
      'plastic:toggle-docs'
      'plastic:toggle-code'
      'plastic:toggle-comments'
      'plastic:toggle-selections-debug'
    ]

  deactivate: ->
    @subscriptions.dispose()

  serialize: ->

  addCommands: (commands) ->
    handler = (command) =>
      (event) =>
        bridge.send "command", command, event

    for command in commands
      @subscriptions.add(atom.commands.add 'atom-workspace', command, handler(command))

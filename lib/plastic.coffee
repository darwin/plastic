require './boot'

path = require 'path'
PlasticEditorView = require './plastic-editor-view'
{CompositeDisposable} = require 'atom'
bridge = require './bridge'
apis = require './apis'

plasticEditorOpener = (uri) ->
  if path.extname(uri) is '.cljs'
    editorView = new PlasticEditorView({uri: uri})
    editorView

module.exports = Plastic =
  subscriptions: null

  activate: (state) ->
    initPlastic = ->
      bridge.send "apis", apis
      bridge.send "init", state # TODO: unserialize our part and pass rest
      
    setTimeout initPlastic, 1000 # TODO: this is temporary, give figwheel and cljs-devtools some time to intialize

    atom.workspace.addOpener plasticEditorOpener

    @subscriptions = new CompositeDisposable

    @addCommands [
      'plastic:toggle-headers'
      'plastic:toggle-docs'
      'plastic:toggle-code'
      'plastic:toggle-text-input-debug'
      'plastic:toggle-text-output-debug'
      'plastic:toggle-parser-debug'
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

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

    @addCommands [
      'quark:toggle-docs'
      'quark:toggle-code'
      'quark:toggle-headers-debug'
      'quark:toggle-docs-debug'
      'quark:toggle-code-debug'
      'quark:toggle-plaintext-debug'
      'quark:toggle-parser-debug'
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
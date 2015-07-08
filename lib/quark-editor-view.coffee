path = require 'path'
{View} = require 'space-pen'
{Disposable, TextEditor} = require 'atom'
bridge = require './bridge'
{ScrollView, TextEditorView} = require 'atom-space-pen-views'

lastId = 0

module.exports =
class QuarkEditorView extends ScrollView
  @content: ->
    @div class: 'quark-editor-view', tabindex: -1, =>
      @div class: 'react-land'
      @div class: 'mini-editor'

  initialize: ({@uri}={}) ->
    super
    lastId += 1
    @id = lastId
    
    @createMiniEditor()

    bridge.send "register-editor", @
    
    @addCommands [
      'quark:move-left'
      'quark:move-right'
      'quark:move-up'
      'quark:move-down'
      'quark:level-up'
      'quark:level-down'
    ]

  createMiniEditor: ->
    @miniEditor = new TextEditor({})
    @miniEditorElement = atom.views.getView(@miniEditor)
    @find('.mini-editor').append(@miniEditorElement)
    
  detached: ->
    bridge.send "unregister-editor", @

  serialize: ->
    # deserializer: 'QuarkView'
    # version: 2
    # uri: @uri

  addCommands: (commands) ->
    handler = (command) =>
      (event) =>
        bridge.send "editor-command", @, command, event
    
    for command in commands
      atom.commands.add @element, command, handler(command)

  getTitle: ->
    if sessionPath = @uri
      path.basename(sessionPath)
    else
      'untitled'

  getLongTitle: ->
    if sessionPath = @uri
      fileName = path.basename(sessionPath)
      directory = atom.project.relativize(path.dirname(sessionPath))
      directory = if directory.length > 0 then directory else path.basename(path.dirname(sessionPath))
      "#{fileName} - #{directory}"
    else
      'untitled'

  getPath: -> @uri

  isModified: -> false

  isEmpty: -> false

  isEqual: (other) ->
    other instanceof QuarkEditorView

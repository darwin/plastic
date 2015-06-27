path = require 'path'
{View, $} = require 'space-pen'
{Disposable} = require 'atom'
#{Subscriber} = require 'emissary'
bridge = require './bridge'

lastId = 0

module.exports =
class QuarkEditorView extends View
  @content: ->
    @div class: 'quark-editor-view', tabindex: -1

  initialize: ({@uri}={}) ->
    lastId += 1
    @id = lastId

    bridge.send "register-editor", @

  detached: ->
    bridge.send "unregister-editor", @

  serialize: ->
    # deserializer: 'QuarkView'
    # version: 2
    # uri: @uri

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

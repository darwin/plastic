path = require 'path'
{View} = require 'space-pen'
{Disposable, TextEditor} = require 'atom'
bridge = require './bridge'
{ScrollView} = require 'atom-space-pen-views'

lastId = 0

monkeyPatchPresenterInstance = (editorView) ->
  presenter = editorView.component.presenter
  throw "presenter on editorView.component not found" unless presenter
  originalMethod = presenter.updateContentDimensions
  throw "presenter.updateContentDimensions not found" unless originalMethod
  presenter.updateContentDimensions = ->
    if @lineHeight?
      oldContentHeight = @contentHeight
      @contentHeight = @lineHeight * @model.getScreenLineCount()

    if @baseCharacterWidth?
      oldContentWidth = @contentWidth
      clip = @model.tokenizedLineForScreenRow(@model.getLongestScreenRow())?.isSoftWrapped()
      @contentWidth = @pixelPositionForScreenPosition([@model.getLongestScreenRow(), @model.getMaxScreenLineLength()], clip).left
      @contentWidth += @scrollLeft
      @contentWidth += 1 unless @model.isSoftWrapped() # account for cursor width

    if @contentHeight isnt oldContentHeight
      @updateHeight()
      @updateScrollbarDimensions()
      @updateScrollHeight()

    if @contentWidth isnt oldContentWidth
      # <<<<<<<<<< next line seamlesly expands editor content frame as user is typing
      @setContentFrameWidth(@contentWidth+1) # +1px for cursor
      # >>>>>>>>>>
      @updateScrollbarDimensions()
      @updateScrollWidth()

monkeyPatchEditorInstance = (editorView) ->
  originalMethod = editorView.mountComponent
  throw "editorView.mountComponent not found" unless originalMethod
  monkeyPatchPresenterInstance(editorView)
  editorView.mountComponent = ->
    originalMethod.apply(@, arguments)
    monkeyPatchPresenterInstance(@)

module.exports =
class PlasticEditorView extends ScrollView
  @content: ->
    @div class: 'plastic-editor-view', tabindex: -1, =>
      @div class: 'react-land'

  initialize: ({@uri}={}) ->
    super
    lastId += 1
    @id = lastId

    @createMiniEditor()

    bridge.send "register-editor", @

    @addCommands [
      'plastic:spatial-left'
      'plastic:spatial-right'
      'plastic:spatial-up'
      'plastic:spatial-down'
      'plastic:structural-up'
      'plastic:structural-down'
      'plastic:structural-left'
      'plastic:structural-right'
      'plastic:stop-editing'
      'plastic:prev-token'
      'plastic:next-token'
      'plastic:backspace'
      'plastic:space'
      'plastic:enter'
    ]

  createMiniEditor: ->
    @miniEditor = new TextEditor(softWrapped: false, tabLength: 2, softTabs: true, lineNumberGutterVisible: false)
    @miniEditorView = atom.views.getView(@miniEditor)
    monkeyPatchEditorInstance(@miniEditorView)

  detached: ->
    bridge.send "unregister-editor", @

  serialize: ->
    # deserializer: 'PlasticView'
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
    other instanceof PlasticEditorView

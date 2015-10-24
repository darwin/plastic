path = require 'path'
{View, $, $$, $$$} = require 'space-pen'
{Disposable, TextEditor} = require 'atom'
{ScrollView} = require 'atom-space-pen-views'
bridge = require './bridge'

lastId = 0

monkeyPatchPresenterInstance = (editorView) ->
  presenter = editorView.component.presenter
  throw "presenter on editorView.component not found" unless presenter
  originalMethod = presenter.updateHorizontalDimensions
  throw "presenter.updateHorizontalDimensions not found" unless originalMethod
  presenter.updateHorizontalDimensions = ->
    if @baseCharacterWidth?
      oldContentWidth = @contentWidth
      clip = @model.tokenizedLineForScreenRow(@model.getLongestScreenRow())?.isSoftWrapped()
      @contentWidth = @pixelPositionForScreenPosition([@model.getLongestScreenRow(), @model.getMaxScreenLineLength()], clip).left
      @contentWidth += @scrollLeft
      @contentWidth += 1 unless @model.isSoftWrapped() # account for cursor width

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

    @id = ++lastId

    $(@element).attr("data-pevid", @id)

    @createMiniEditor()

    bridge.send "register-editor", @id, @uri

    @addOps [
      'plastic:abort-keybinding'
      'plastic:nop'
      'plastic:spatial-left'
      'plastic:spatial-right'
      'plastic:spatial-up'
      'plastic:spatial-down'
      'plastic:structural-up'
      'plastic:structural-down'
      'plastic:structural-left'
      'plastic:structural-right'
      'plastic:stop-editing'
      'plastic:prev-interest'
      'plastic:next-interest'
      'plastic:backspace'
      'plastic:delete'
      'plastic:alt-delete'
      'plastic:space'
      'plastic:enter'
      'plastic:alt-enter'
      'plastic:open-list'
      'plastic:open-vector'
      'plastic:open-map'
      'plastic:open-set'
      'plastic:open-fn'
      'plastic:open-meta'
      'plastic:open-deref'
      'plastic:open-quote'
      'plastic:start-editing'
      'plastic:stop-editing'
      'plastic:toggle-editing'
      'plastic:activate-puppets'
      'plastic:deactivate-puppets'
      'plastic:toggle-puppets'
      'plastic:undo'
      'plastic:redo'
    ]

  createMiniEditor: ->
    @miniEditor = new TextEditor(softWrapped: false, tabLength: 2, softTabs: true, lineNumberGutterVisible: false)
    @miniEditorView = atom.views.getView(@miniEditor)
    monkeyPatchEditorInstance(@miniEditorView)
    $(@element).data('mini-editor', @miniEditor)
    $(@element).data('mini-editor-view', @miniEditorView)

  detached: ->
    bridge.send "unregister-editor", @id

  serialize: ->
    # deserializer: 'PlasticView'
    # version: 2
    # uri: @uri

  addOps: (ops) =>
    handler = (op) =>
      (event) =>
        bridge.send "editor-op", @id, op, event

    for op in ops
      atom.commands.add @element, op, handler(op)

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

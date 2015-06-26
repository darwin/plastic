require("./boot")

QuarkView = require './quark-view'
{CompositeDisposable} = require 'atom'

module.exports = Quark =
  quarkView: null
  modalPanel: null
  subscriptions: null

  activate: (state) ->
    @quarkView = new QuarkView(state.quarkViewState)
    @modalPanel = atom.workspace.addModalPanel(item: @quarkView.getElement(), visible: false)

    @subscriptions = new CompositeDisposable

    @subscriptions.add atom.commands.add 'atom-workspace', 'quark:toggle': => @toggle()
    @subscriptions.add atom.commands.add 'atom-workspace', 'quark:wake': => @wake()

  deactivate: ->
    @modalPanel.destroy()
    @subscriptions.dispose()
    @quarkView.destroy()

  serialize: ->
    quarkViewState: @quarkView.serialize()

  wake: ->
    console.info "Quark is here"

  toggle: ->
    if @modalPanel.isVisible()
      console.log 'hide Quark!'
      @modalPanel.hide()
    else
      console.log 'show Quark!'
      @modalPanel.show()

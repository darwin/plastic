require("./_build/goog/bootstrap/nodejs.js")

# ---------------- START OF THE HACK
# hack our way to include react without patching reagent
oldNodeGlobalRequire = goog.nodeGlobalRequire

goog.nodeGlobalRequire = (path) ->
  return oldNodeGlobalRequire(path) unless path == "../lib/_build/react.inc.js"
  console.warn "cljs-patch: prevented loading ", path

require("./_build/react.inc.js")
# ---------------- END OF THE HACK

require("./_build/cljs_deps.js")

goog.require("quark.init")

QuarkView = require './quark-view'
{CompositeDisposable} = require 'atom'

module.exports = Quark =
  quarkView: null
  modalPanel: null
  subscriptions: null

  activate: (state) ->
    @quarkView = new QuarkView(state.quarkViewState)
    @modalPanel = atom.workspace.addModalPanel(item: @quarkView.getElement(), visible: false)

    # Events subscribed to in atom's system can be easily cleaned up with a CompositeDisposable
    @subscriptions = new CompositeDisposable

    # Register command that toggles this view
    @subscriptions.add atom.commands.add 'atom-workspace', 'quark:toggle': => @toggle()

  deactivate: ->
    @modalPanel.destroy()
    @subscriptions.dispose()
    @quarkView.destroy()

  serialize: ->
    quarkViewState: @quarkView.serialize()

  toggle: ->
    console.log 'Quark was toggled!'

    if @modalPanel.isVisible()
      @modalPanel.hide()
    else
      @modalPanel.show()

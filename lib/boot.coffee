require("./_build/goog/bootstrap/nodejs.js")

prevImportScript = goog.global.CLOSURE_IMPORT_SCRIPT

goog.global.CLOSURE_IMPORT_SCRIPT = (src) ->
  # console.log "loading deps:", src
  prevImportScript(src)

# ---------------- START OF THE HACK
# hack our way to include react without patching reagent
oldNodeGlobalRequire = goog.nodeGlobalRequire

goog.nodeGlobalRequire = (path) ->
  return oldNodeGlobalRequire(path) unless path == "../lib/_build/react.inc.js"
  console.log "cljs-patch: prevented loading ", path

global.React = require("./_build/react.inc.js")
# ---------------- END OF THE HACK

require("./_build/cljs_deps.js")

goog.require("quark.init")
goog.require("quark.main")

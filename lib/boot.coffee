require("./_build/goog/bootstrap/nodejs.js")
# ---------------- START OF THE HACK
# hack our way to include react without patching reagent
oldNodeGlobalRequire = goog.nodeGlobalRequire

goog.nodeGlobalRequire = (path) ->
  return oldNodeGlobalRequire(path) unless path == "../lib/_build/react.inc.js"
  console.log "cljs-patch: prevented loading ", path

require("./_build/react.inc.js")
# ---------------- END OF THE HACK

require("./_build/cljs_deps.js")

goog.require("quark.init")

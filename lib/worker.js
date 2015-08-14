this.CLOSURE_IMPORT_SCRIPT = (function(global) {
  return function(src) {
    global['importScripts'](src);
    return true;
  };
})(this);

this.CLOSURE_BASE_PATH = "_build/worker/goog/";

importScripts('_build/worker/goog/base.js');
//importScripts('_build/main/goog/bootstrap/nodejs.js')

oldNodeGlobalRequire = goog.nodeGlobalRequire;

goog.nodeGlobalRequire = function (path) {
  if (path != "../lib/_build/main/react.inc.js") {
    //return oldNodeGlobalRequire(path);
    console.log("???", path);
    return;
  }
  console.log("worker-patch: prevented loading ", path);
};

importScripts('_build/worker/cljs_deps.js');

//importScripts('_build/worker/plastic.js');

goog.require("plastic.worker.loop");
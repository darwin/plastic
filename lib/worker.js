// all this hackery is here for us to be able to include generated _build/worker/plastic.js
// it contains CLOSURE_UNCOMPILED_DEFINES which is what we want

this.CLOSURE_IMPORT_SCRIPT = (function(global) {
  return function(src) {
    global['importScripts'](src);
    return true;
  };
})(this);

this.CLOSURE_BASE_PATH = "_build/worker/goog/";

importScripts('_build/worker/goog/base.js');

oldNodeGlobalRequire = goog.nodeGlobalRequire;

goog.nodeGlobalRequire = function (path) {
  if (!path) {
    return;
  }
  if (path.match(/react\.inc\.js$/)) {
    console.log("worker-patch: prevented loading ", path);
    return;
  }
  if (path.match(/^path$/)) {
    console.log("worker-patch: emulating node.js require('path') call");
    return {
      join: function() { 
        var args = Array.prototype.slice.call(arguments);
        return args.join("/"); 
      },
      resolve: function(path) { return path; }
    };
  }
  if (path.match(/^source-map-support$/)) {
    console.log("worker-patch: emulating node.js require('source-map-support') call");
    return {
      install: function() {}
    };
  }
  if (path.match(/cljs_deps\.js$/)) {
    importScripts(path);
    return;
  }
  if (path.match(/nodejs\.js$/)) {
    console.log("worker-patch: prevented loading ", path);
    return;
  }
  if (oldNodeGlobalRequire) {
    return oldNodeGlobalRequire(path);
  }
  return null;
};

oldGoogRequire = goog.require;

goog.require = function(name) {
  if (name=="cljs.nodejscli") {
    console.log("worker-patch: ignore goog.require('cljs.nodejscli') call");
    return;
  }
  return oldGoogRequire(name);
};

this.require = goog.nodeGlobalRequire;

importScripts('_build/worker/plastic.js');

goog.require("plastic.worker.loop");
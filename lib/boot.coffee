dev = true
buildName = if dev then "dev" else "main"

require("./_build/#{buildName}/plastic.js")

goog.require("plastic.main.loop")
goog.require("plastic.worker.loop") if dev
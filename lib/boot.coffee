require("./_build/main/plastic.js")

goog.require("plastic.dev")
goog.require("plastic.main.loop")
goog.require("plastic.worker.loop") if plastic.env.run_worker_on_main_thread
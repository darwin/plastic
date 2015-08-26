(ns n1.doc)

(defn log-ex
  "Middleware which catches and prints any handler-generated exceptions to console.
  Handlers are called from within a core.async go-loop, and core.async produces
  a special kind of hell when in comes to stacktraces. By the time an exception
  has passed through a go-loop its stack is mangled beyond repair and you'll
  have no idea where the exception was thrown.
  So this middleware catches and prints to stacktrace before the core.async sausage
  machine has done its work.
  "
  [handler]
  (fn log-ex-handler
    [db v]
    (warn "re-frame: use of \"log-ex\" is deprecated.")
    (try
      (handler db v)
      (catch :default e                                     ;; ooops, handler threw
        (do
          (.error js/console (.-stack e))
          (throw e))))))

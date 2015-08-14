(ns plastic.main.render)

(defmacro log-render [title params & body]
  `(do
     (if plastic.env/log-rendering
       (plastic.logging/fancy-log "RENDER" ~title ~params))
     (let [res# ~@body]
       res#)))
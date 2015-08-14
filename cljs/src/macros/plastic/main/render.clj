(ns plastic.main.render)

(defmacro log-render [title params & body]
  `(do
     (plastic.logging/fancy-log "RENDER" ~title ~params)
     (plastic.main.editor.render.utils/inc-indent)
     (let [res# ~@body]
       (plastic.main.editor.render.utils/dec-indent)
       res#)))
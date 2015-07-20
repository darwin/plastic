(ns plastic.macros.render)

(defmacro log-render [title params & body]
  `(do
     (plastic.macros.logging/log (str (plastic.cogs.editor.render.utils/str-indent) "R! " ~title) ~params)
     (plastic.cogs.editor.render.utils/inc-indent)
     (let [res# ~@body]
       (plastic.cogs.editor.render.utils/dec-indent)
       res#)))
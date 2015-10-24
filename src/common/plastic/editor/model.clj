(ns plastic.editor.model)

; -------------------------------------------------------------------------------------------------------------------

(defmacro editor-react! [context editor-subscription derefable f]
  `(let [previous-value# (volatile! ::not-initialized)]
     (plastic.util.reactions/react!
       (let [editor-subscription# ~editor-subscription
             _# (reagent.ratom/run editor-subscription#)
             value# @~derefable
             editor# @editor-subscription#]
         (when-not (identical? @previous-value# value#)
           (if editor#
             (let [editor-with-context# (plastic.editor.model/set-context editor# ~context)]
               (vreset! previous-value# value#)
               (~f editor-with-context# value#))))))))
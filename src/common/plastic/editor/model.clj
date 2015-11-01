(ns plastic.editor.model
  (:require [plastic.util.reactions :refer [react!]]))

; -------------------------------------------------------------------------------------------------------------------

(defmacro editor-react! [context editor-subscription derefable f]
  `(let [previous-value# (volatile! ::not-initialized)]
     (react!
       (let [editor-subscription# ~editor-subscription
             _# (reagent.ratom/run editor-subscription#)                                                              ; insanity
             value# @~derefable
             editor# @editor-subscription#]
         (when-not (identical? @previous-value# value#)
           (if (and editor# (not (and (= @previous-value# ::not-initialized) (nil? value#))))                         ; ugly edge cases, this whole reaction nightmare must go away
             (let [editor-with-context# (plastic.editor.model/set-context editor# ~context)]
               (vreset! previous-value# value#)
               (~f editor-with-context# value#))))))))
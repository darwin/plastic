(ns plastic.worker.editor.model.xforming
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]
            [plastic.worker.editor.model.report :as report]
            [plastic.worker.editor.model :as editor :refer [valid-editor?]]
            [plastic.worker.editor.toolkit.id :as id]))

(defn make-state [loc report]
  [loc report])

(defn make-initial-state [editor]
  {:pre [(valid-editor? editor)]}
  (let [parse-tree (editor/get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)]
    (make-state root-loc (report/make))))

; -- transducers ----------------------------------------------------------------------------------------------------
;
; see http://clojure.org/transducers[1]

(defn zipping-xform [reducing-fn]
  (fn
    ([] (reducing-fn))                                                                                                ; transduction init, see [1]
    ([result] (reducing-fn result))                                                                                   ; transduction completion, see [1]
    ([state zipop]                                                                                                    ; transduction step, see [1]
     (let [op-fn (first zipop)
           op-args (conj (vec (rest zipop)) state)
           op-res (apply op-fn op-args)]
       (reducing-fn state op-res)))))

(defn zipping-reducing-fn
  ([state] state)                                                                                                     ; completion
  ([state new-state]                                                                                                  ; in each step
   new-state))

(defn apply-zipops [editor zipops]
  (let [initial-state (make-initial-state editor)
        final-state (transduce zipping-xform zipping-reducing-fn initial-state zipops)
        final-parse-tree (z/root (first final-state))
        final-report (second final-state)]
    (-> editor
      (editor/set-xform-report final-report)
      (editor/set-parse-tree final-parse-tree))))

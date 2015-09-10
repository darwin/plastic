(ns plastic.worker.editor.model.xforming
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.editor.model.report :as report]
            [plastic.worker.editor.model :as editor :refer [valid-editor?]]
            [meld.zip :as zip]))

(defn make-state [loc report]
  [loc report])

(defn get-loc [state]
  (first state))

(defn get-report [state]
  (second state))

(defn make-initial-state [editor]
  {:pre [(valid-editor? editor)]}
  (let [meld (editor/get-meld editor)
        root-loc (zip/zip meld)]
    (make-state root-loc (report/make))))

(defn remove-empty-units [loc]
  {:pre [(zip/unit? loc)]}
  loc ; TODO:
  #_(if (zip-utils/contains-only-spaces? loc)
    (let [after-loc (z/remove loc)
          next-form-loc (zip-utils/skip z/next zip-utils/form? after-loc)]
      (if next-form-loc
        (recur next-form-loc)
        after-loc))
    (if-let [next-loc (z/right loc)]
      (recur next-loc)
      loc)))

(defn sanitize-zipper [top-loc]
  (-> top-loc
      zip/down
      remove-empty-units))

(defn sanitize-if-needed [state]
  (let [report (get-report state)]
    (if (and (empty? (:removed report)) (empty? (:moved report)))
      state
      (let [top (zip/top (get-loc state))]
        (make-state (sanitize-zipper top) report)))))

(defn commit-state [editor state]
  {:pre [(valid-editor? editor)]}
  (if (nil? state)
    editor
    (let [sanitized-state (sanitize-if-needed state)
          meld (zip/unzip (get-loc sanitized-state))
          report (get-report sanitized-state)]
      (-> editor
        (editor/set-xform-report report)
        (editor/set-meld meld)))))

; -------------------------------------------------------------------------------------------------------------------

(defn apply-ops [editor f coll]
  {:pre [(valid-editor? editor)]}
  (let [initial-state (make-initial-state editor)]
    (if-let [state (reduce f initial-state coll)]
      (commit-state editor state)
      editor)))

(defn apply-op [editor zip-op & args]
  {:pre [(valid-editor? editor)]}
  (apply-ops editor #(apply %2 (conj (vec args) %1)) [zip-op]))

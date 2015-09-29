(ns plastic.worker.frame.undo
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.worker :refer [main-dispatch-args dispatch-args dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.util.booking :as booking]))

; -------------------------------------------------------------------------------------------------------------------

(defn open-undo-session [storage description]
  (assoc-in storage [:undo-state] {:description description}))

(defn set-undo-report [storage report]
  (assoc-in storage [:undo-state :xform-report] report))

; -------------------------------------------------------------------------------------------------------------------
; we have to keep independent undo/redo queue for each editor

(defonce book (booking/make-booking))

(defn open-undo-session! [editor-id description]
  (booking/update-item! book editor-id open-undo-session description))

(defn set-undo-report! [editor-id report]
  (booking/update-item! book editor-id set-undo-report report))

(defn vacuum-undo-summary []
  (let [result (process @book []
                 (fn [accum [id record]]
                   (let [state (:undo-state record)]
                     (if (:xform-report state)
                       (conj accum (assoc state :editor-id id))
                       accum))))]
    (when-not (empty? result)
      (reset! book {})
      result)))

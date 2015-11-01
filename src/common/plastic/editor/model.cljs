(ns plastic.editor.model
  (:require-macros [plastic.editor.model :refer [editor-react!]])
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.common :refer-macros [process]]
            [plastic.frame :refer [subscribe]]
            [plastic.util.helpers :refer [select-values]]
            [plastic.util.booking :as booking]
            [plastic.util.reactions :refer [register-reaction unregister-reaction dispose-reaction!
                                            unregister-and-dispose-all-reactions!]]))

; -------------------------------------------------------------------------------------------------------------------

(defprotocol IEditor)

(defrecord Editor [id uri]
  IEditor)

(extend-protocol IHash
  Editor
  (-hash [this] (goog/getUid this)))

(defn make [editor-id editor-uri]
  (Editor. editor-id editor-uri))

; -------------------------------------------------------------------------------------------------------------------

(defn valid-editor? [editor]
  (satisfies? IEditor editor))

(defn valid-editor-id? [editor-id]
  (pos? editor-id))

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [editor]
  {:post [(valid-editor-id? %)]}
  (:id editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-context [editor]
  (:context editor))

(defn set-context [editor context]
  (assoc editor :context context))

(defn strip-context [editor]
  (dissoc editor :context))

; -------------------------------------------------------------------------------------------------------------------

(defn selector-matches-editor? [editor-id selector]
  {:pre [(valid-editor-id? editor-id)]}
  (cond
    (vector? selector) (some #{editor-id} selector)
    (set? selector) (contains? selector editor-id)
    :default (= editor-id selector)))

(defn apply-to-editors [context db selector f & args]
  (update db :editors #(process (keys %) %
                        (fn [editors editor-id]
                          (or
                            (if (selector-matches-editor? editor-id selector)
                              (let [editor (set-context (get editors editor-id) context)]
                                (if-let [new-editor (apply f editor args)]
                                  (if-not (identical? editor new-editor)
                                    (assoc editors editor-id (strip-context new-editor))))))
                            editors)))))

; -------------------------------------------------------------------------------------------------------------------

(defn subscribe! [editor subscription-spec f]
  (let [editor-id (get-id editor)
        context (get-context editor)
        {:keys [aux]} context
        subscription (subscribe context subscription-spec)
        editor-sub (subscribe context [:editor editor-id])
        reaction (editor-react! context editor-sub subscription f)]
    (booking/update-item! aux editor-id register-reaction reaction)
    reaction))

(defn unsubscribe! [editor reaction]
  (let [editor-id (get-id editor)
        context (get-context editor)
        {:keys [aux]} context]
    (booking/update-item! aux editor-id unregister-reaction reaction)
    (dispose-reaction! reaction)))

(defn unsubscribe-all! [context editor-id]
  (let [{:keys [aux]} context]
    (booking/update-item! aux editor-id unregister-and-dispose-all-reactions!)))
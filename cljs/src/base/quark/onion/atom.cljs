(ns quark.onion.atom
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [dispatch react!]]))

(defonce ids->views (atom {}))

(defn register-view [editor-id atom-view]
  (swap! ids->views assoc editor-id atom-view))

(defn unregister-view [editor-id]
  (swap! ids->views dissoc editor-id))

; -------------------------------------------------------------------------------------------

(defmulti process (fn [command & _] (keyword command)))

(defmethod process :default [command]
  (error (str "Invalid onion message '" command "'")))

(defmethod process :init [_]
  (log "init"))

(defmethod process :register-editor [_ atom-view]
  (let [editor-id (.-id atom-view)
        editor-def {:id  editor-id
                    :uri (.-uri atom-view)}]
    (register-view editor-id atom-view)
    (dispatch :add-editor editor-id editor-def)))

(defmethod process :unregister-editor [_ atom-view]
  (let [editor-id (.-id atom-view)]
    (dispatch :remove-editor editor-id)
    (unregister-view editor-id)))

; -------------------------------------------------------------------------------------------

(defn ^:export send [& args]
  (apply process args))


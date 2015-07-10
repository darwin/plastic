(ns quark.onion.inface
  (:require [quark.onion.api :as api]
            [quark.util.dom-shim]
            [clojure.string :as string])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [dispatch react!]]))

(defonce ids->views (atom {}))

(defn register-view [editor-id atom-view]
  (swap! ids->views assoc editor-id atom-view))

(defn unregister-view [editor-id]
  (swap! ids->views dissoc editor-id))

(defn find-mount-point [dom-node]
  (let [react-land-dom-nodes (.getElementsByClassName dom-node "react-land")]
    (assert react-land-dom-nodes)
    (assert (= (count react-land-dom-nodes) 1))
    (first react-land-dom-nodes)))

; -------------------------------------------------------------------------------------------

(defmulti process (fn [command & _] (keyword command)))

(defmethod process :default [command]
  (error (str "Invalid onion message '" command "'")))

(defmethod process :apis [_ apis]
  (api/register-apis! apis))

(defmethod process :init [_ state]
  (dispatch :init (js->clj state :keywordize-keys true)))

(defmethod process :register-editor [_ atom-view]
  (let [editor-id (.-id atom-view)
        editor-def {:id  editor-id
                    :uri (.-uri atom-view)}]
    (register-view editor-id atom-view)
    (dispatch :add-editor editor-id editor-def)
    (dispatch :mount-editor editor-id (find-mount-point (.-element atom-view)))))

(defmethod process :unregister-editor [_ atom-view]
  (let [editor-id (.-id atom-view)]
    (dispatch :remove-editor editor-id)
    (unregister-view editor-id)))

(defmethod process :editor-command [_ atom-view command event]
  (let [editor-id (.-id atom-view)
        internal-command (keyword (string/replace command #"^quark:" ""))]
    (dispatch :editor-command editor-id internal-command)
    (.stopPropagation event)))

(defmethod process :command [_ command event]
  (let [internal-command (keyword (string/replace command #"^quark:" ""))]
    (dispatch :command internal-command)
    (.stopPropagation event)))

; -------------------------------------------------------------------------------------------

(defn ^:export send [& args]
  (apply process args))
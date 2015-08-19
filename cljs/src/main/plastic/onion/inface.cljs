(ns plastic.onion.inface
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.main :refer [dispatch react!]])
  (:require [plastic.main.frame]
            [plastic.onion.api :as api]
            [plastic.util.dom.shim]
            [clojure.string :as string]))

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

; -------------------------------------------------------------------------------------------------------------------

(defn init [state]
  (dispatch :init (js->clj state :keywordize-keys true)))

(defn register-editor [atom-view]
  (let [editor-id (.-id atom-view)
        editor-def {:id  editor-id
                    :uri (.-uri atom-view)}]
    (register-view editor-id atom-view)
    (dispatch :add-editor editor-id editor-def)
    (dispatch :mount-editor editor-id (find-mount-point (.-element atom-view)))))

(defn unregister-editor [atom-view]
  (let [editor-id (.-id atom-view)]
    (dispatch :remove-editor editor-id (find-mount-point (.-element atom-view)))
    (unregister-view editor-id)))

(defn editor-op [atom-view command event]
  (let [editor-id (.-id atom-view)
        internal-command (keyword (string/replace command #"^plastic:" ""))]
    (if (= internal-command :abort-keybinding)
      (do
        (log "abort keybinding")
        (.abortKeyBinding event))
      (do
        (dispatch :editor-op editor-id internal-command)
        (.stopPropagation event)))))

(defn command [command event]
  (let [internal-command (keyword (string/replace command #"^plastic:" ""))]
    (dispatch :command internal-command)
    (.stopPropagation event)))

; -------------------------------------------------------------------------------------------------------------------

(def inface
  {:apis              api/register-apis!
   :init              init
   :register-editor   register-editor
   :unregister-editor unregister-editor
   :editor-op         editor-op
   :command           command})

(defn ^:export send [msg-id & args]
  (when (or plastic.env.log-onion plastic.env.log-onion-inface)
    (if (= plastic.env.*current-thread* "WORK")
      (js-debugger))
    (fancy-log "ONION IN" msg-id args))
  (if-let [handler (get inface (keyword msg-id))]
    (apply handler args)
    (error (str "Invalid onion message '" msg-id "'"))))
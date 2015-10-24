(ns plastic.onion.inface
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.frame :refer [dispatch]])
  (:require [plastic.util.dom.shim]
            [clojure.string :as string]
            [plastic.env :as env :include-macros true]
            [plastic.main.editor.render :as render]
            [plastic.util.dom :as dom]))

; -------------------------------------------------------------------------------------------------------------------

(defn init-onion [context]
  context)

; -------------------------------------------------------------------------------------------------------------------

(defn init [context state]
  (dispatch context [:init (js->clj state :keywordize-keys true)]))

(defn register-editor [context editor-id editor-uri]
  (dispatch context [:add-editor editor-id editor-uri]
    (fn [db]
      (let [mount-node (dom/find-react-mount-point editor-id)]
        (render/mount-editor context mount-node editor-id)
        db))))

(defn unregister-editor [context editor-id]
  (let [mount-node (dom/find-react-mount-point editor-id)]
    (render/unmount-editor context mount-node))
  (dispatch context [:remove-editor editor-id]))

(defn editor-op [context editor-id command event]
  {:pre [(number? editor-id)]}
  (let [internal-command (keyword (string/replace command #"^plastic:" ""))]
    (if (keyword-identical? internal-command :abort-keybinding)
      (do
        (log "abort keybinding")
        (.abortKeyBinding event))
      (do
        (dispatch context [:editor-op editor-id internal-command])
        (.stopPropagation event)))))

(defn command [context command event]
  (let [internal-command (keyword (string/replace command #"^plastic:" ""))]
    (dispatch context [:command internal-command])
    (.stopPropagation event)))

; -------------------------------------------------------------------------------------------------------------------

(def inface
  {:init              init
   :register-editor   register-editor
   :unregister-editor unregister-editor
   :editor-op         editor-op
   :command           command})

(defn send [context msg-id & args]
  (when (env/or context :log-onion)
    (fancy-log "ONION" msg-id args))
  (if-let [handler (get inface (keyword msg-id))]
    (apply handler context args)
    (error (str "Invalid onion message '" msg-id "'"))))
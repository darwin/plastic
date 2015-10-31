(ns plastic.onion.inline-editor
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.onion.inline-editor :refer [update-inline-editor-synchronously]]
                   [plastic.frame :refer [dispatch]])
  (:require [plastic.onion.api :refer [$ atom-api]]
            [plastic.main.editor.model :as editor]
            [plastic.util.dom :as dom]
            [plastic.env :as env :include-macros true]))

; -------------------------------------------------------------------------------------------------------------------

(def log-label "INLINE EDITOR")

(defonce book-keeping (atom {}))

(defn get-atom-inline-editor-instance [editor-id]
  (let [$atom-editor-view ($ (dom/find-plastic-editor-view editor-id))
        mini-editor (.data $atom-editor-view "mini-editor")]
    (assert mini-editor)
    mini-editor))

(defn get-atom-inline-editor-view-instance [editor-id]
  (let [$atom-editor-view ($ (dom/find-plastic-editor-view editor-id))
        mini-editor-view (.data $atom-editor-view "mini-editor-view")]
    (assert mini-editor-view)
    mini-editor-view))

(defn append-inline-editor [editor-id dom-node]
  (let [inline-editor-view (get-atom-inline-editor-view-instance editor-id)]
    (.append ($ dom-node) inline-editor-view)))

(defn is-inline-editor-focused? [editor-id]
  (let [inline-editor-view (get-atom-inline-editor-view-instance editor-id)]
    (.hasClass ($ inline-editor-view) "is-focused")))

(defn focus-inline-editor [editor-id]
  (let [inline-editor-view (get-atom-inline-editor-view-instance editor-id)]
    (.focus inline-editor-view)))

(def known-editor-modes #{:switcher :symbol :keyword :string :regexp})

(defn editor-mode-to-class-name [editor-mode]
  {:pre [(contains? known-editor-modes editor-mode)]}
  (str "plastic-mode-" (name editor-mode)))

(defn editor-mode-to-token-type-class-name [editor-mode]
  {:pre [(contains? known-editor-modes editor-mode)]}
  (name editor-mode))

(defn class-name-to-editor-mode [class-name]
  {:post [(contains? known-editor-modes %)]}
  (if-let [match (re-find #"^plastic-mode-(.*)$" class-name)]
    (keyword (second match))))

(def known-editor-modes-classes (map editor-mode-to-class-name known-editor-modes))
(def known-editor-types-classes (map editor-mode-to-token-type-class-name known-editor-modes))

(defn sync-token-type-with-editor-mode [$token editor-mode]
  (-> $token
    (.removeClass (apply str (interpose " " known-editor-types-classes)))
    (.addClass (editor-mode-to-token-type-class-name editor-mode))))

(defn set-token-raw-text [$token text]
  (let [$raw (.children $token ".raw")]
    (.text $raw text)))

(defn set-editor-mode-as-class-name [inline-editor-view editor-mode]
  (sync-token-type-with-editor-mode (.closest ($ inline-editor-view) ".token") editor-mode)
  (-> ($ inline-editor-view)
    (.removeClass (apply str (interpose " " known-editor-modes-classes)))
    (.addClass (editor-mode-to-class-name editor-mode))))

(defn preprocess-text-before-editing [editor-mode text]
  (condp = editor-mode
    ;:keyword (strip-colon text)
    text))

(defn get-inline-editor-mode-from-class [editor-id]
  {:post [(contains? known-editor-modes %)]}
  (let [$inline-editor-view ($ (get-atom-inline-editor-view-instance editor-id))]
    (class-name-to-editor-mode (some #(if (.hasClass $inline-editor-view %) %) known-editor-modes-classes))))

(defn dispatch-command-in-inline-editor [editor-id command]
  (let [inline-editor-view (get-atom-inline-editor-view-instance editor-id)]
    (.dispatch (.-commands atom-api) inline-editor-view command)))

(defn insert-text-into-inline-editor [context editor-id text]
  (let [inline-editor (get-atom-inline-editor-instance editor-id)]
    (.insertText inline-editor text)))

(defn set-editor-mode-and-clear-text [_editor-id inline-editor inline-editor-view mode]
  (let [has-mode? (.hasClass ($ inline-editor-view) (editor-mode-to-class-name mode))]
    (when-not has-mode?
      (set-editor-mode-as-class-name inline-editor-view mode)
      (.setText inline-editor ""))))

(defn update-inline-editor-state [context editor-id inline-editor]
  (let [current-value {:text (.getText inline-editor) :mode (get-inline-editor-mode-from-class editor-id)}
        initial-value (get-in @book-keeping [editor-id :initial-value])
        current-state {:empty?        (.isEmpty inline-editor)
                       :value         current-value
                       :initial-value initial-value
                       :modified?     (not= current-value initial-value)}]
    (dispatch context [:editor-update-inline-editor editor-id current-state])))                                       ; TODO: dispatch

(defn update-puppets! [editor]
  (let [editor-id (editor/get-id editor)
        $atom-editor-view ($ (dom/find-plastic-editor-view editor-id))
        puppets (editor/get-puppets editor)
        selector (dom/build-nodes-selector (seq puppets))
        $puppets (dom/find-all $atom-editor-view selector)
        mode (editor/get-inline-editor-mode editor)
        effective? (editor/get-inline-editor-puppets-effective? editor)
        initial-value (get-in @book-keeping [editor-id :initial-value])
        effective-text (if effective? (editor/get-inline-editor-text editor) (:text initial-value))
        effective-mode (if effective? mode (:mode initial-value))
        context (editor/get-context editor)]
    (if (env/get context :log-inline-editor)
      (fancy-log log-label "updating puppets" effective-mode effective-text $puppets))
    (sync-token-type-with-editor-mode $puppets effective-mode)
    (set-token-raw-text $puppets effective-text)))

(defn update-state! [editor]
  (let [editor-id (editor/get-id editor)]
    (when (get-in @book-keeping [editor-id :active])
      (update-puppets! editor))))

(defn on-did-change [context editor-id inline-editor inline-editor-view]
  (if (.isEmpty inline-editor)
    (.addClass ($ inline-editor-view) "empty")
    (.removeClass ($ inline-editor-view) "empty"))
  (let [text (.getText inline-editor)
        switch-mode (partial set-editor-mode-and-clear-text editor-id inline-editor inline-editor-view)]
    (condp = text
      ":" (switch-mode :keyword)
      "\"" (switch-mode :string)
      "'" (switch-mode :symbol)
      nil))
  (update-inline-editor-state context editor-id inline-editor))                                                       ; this will trigger async update-puppets call, see lifecycle

(defn initial-inline-editor-setup-if-needed [context editor-id inline-editor inline-editor-view]
  (if-not (get @book-keeping editor-id)
    (let [handler (partial on-did-change context editor-id inline-editor inline-editor-view)
          disposable-id (.onDidChange inline-editor handler)]
      (swap! book-keeping assoc editor-id {:on-did-change-disposable-id disposable-id}))))

(defn setup-inline-editor-for-editing [context editor-id setup]
  {:pre [(contains? known-editor-modes (:mode setup))]}
  (let [inline-editor (get-atom-inline-editor-instance editor-id)
        inline-editor-view (get-atom-inline-editor-view-instance editor-id)
        {:keys [mode text]} setup
        initial-text (preprocess-text-before-editing mode text)]
    (initial-inline-editor-setup-if-needed context editor-id inline-editor inline-editor-view)
    (swap! book-keeping update editor-id #(-> %
                                           (assoc :active true)
                                           (assoc :initial-value setup)))
    ; synchronous updates prevent intermittent jumps
    ; editor gets correct dimensions before it gets appended in the DOM
    (update-inline-editor-synchronously inline-editor-view
      (set-editor-mode-as-class-name inline-editor-view mode)
      (.setText inline-editor initial-text)                                                                           ; this will trigger forst on-did-change call, which updates initial editor state in app-db
      (.selectAll inline-editor))))

(defn teardown-inline-editor-editing! [editor-id]
  (swap! book-keeping update editor-id #(-> %
                                         (assoc :active false)
                                         (assoc :initial-value nil))))

(defn activate-inline-editor! [editor]
  (let [editor-id (editor/get-id editor)
        context (editor/get-context editor)]
    (if (env/get context :log-inline-editor)
      (fancy-log log-label "activate inline editor request"))
    (let [$root-view ($ (dom/find-plastic-editor-view editor-id))]
      (when-not (dom/has-class? $root-view "inline-editor-active")
        (if (env/get context :log-inline-editor)
          (fancy-log log-label "setup, append, focus and add \"inline-editor-active\" class to the root-view"))
        (.addClass $root-view "inline-editor-active")
        (let [setup (editor/get-editing-setup editor)]
          (setup-inline-editor-for-editing context editor-id setup)
          (append-inline-editor editor-id $root-view)                                                                 ; temporary, react renderer will relocate it to proper place, see activate-transplantation
          (focus-inline-editor editor-id))))))                                                                        ; it is important to focus inline editor ASAP, so we don't lose keystrokes

(defn deactivate-inline-editor! [editor]
  (let [editor-id (editor/get-id editor)
        context (editor/get-context editor)]
    (if (env/get context :log-inline-editor)
      (fancy-log log-label "deactivate inline editor request"))
    (let [$root-view ($ (dom/find-plastic-editor-view editor-id))]
      (when (dom/has-class? $root-view "inline-editor-active")
        (if (env/get context :log-inline-editor)
          (fancy-log log-label "removing \"inline-editor-active\" class from the root-view"))
        (.removeClass $root-view "inline-editor-active")
        (if (env/get context :log-inline-editor)
          (fancy-log log-label "returning focus back to root-view"))
        (.focus $root-view)
        (teardown-inline-editor-editing! editor-id)
        (dispatch context [:editor-update-inline-editor editor-id nil])))))
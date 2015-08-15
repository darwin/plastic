(ns plastic.onion.atom
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.onion.inface :as inface]
            [plastic.onion.api :refer [$ atom]]
            [plastic.onion.remounter]
            [plastic.onion.api :refer [File]]))

(defonce ^:dynamic *initial-text* nil)
(defonce ^:dynamic *on-did-change-disposables* {})

(defn load-file-content [uri cb]
  {:pre [File]}
  (let [file (File. uri)
        content (.read file)]
    (.then content cb)))

(defn get-plastic-editor-view [editor-id]
  (let [atom-editor-view (get @inface/ids->views editor-id)
        _ (assert atom-editor-view)]
    atom-editor-view))

(defn get-atom-inline-editor-instance [editor-id]
  (let [atom-editor-view (get-plastic-editor-view editor-id)
        mini-editor (.-miniEditor atom-editor-view)]
    (assert mini-editor)
    mini-editor))

(defn get-atom-inline-editor-view-instance [editor-id]
  (let [atom-editor-view (get-plastic-editor-view editor-id)
        mini-editor-view (.-miniEditorView atom-editor-view)]
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

(def known-editor-modes #{:switcher :symbol :keyword :doc :string})

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

(defn sync-editor-mode-with-token-type [inline-editor-view editor-mode]
  (let [$token (.closest ($ inline-editor-view) ".token")]
    (-> $token
      (.removeClass (apply str (interpose " " known-editor-types-classes)))
      (.addClass (editor-mode-to-token-type-class-name editor-mode)))))

(defn set-editor-mode-as-class-name [inline-editor-view editor-mode]
  (sync-editor-mode-with-token-type inline-editor-view editor-mode)
  (-> ($ inline-editor-view)
    (.removeClass (apply str (interpose " " known-editor-modes-classes)))
    (.addClass (editor-mode-to-class-name editor-mode))))

(defn preprocess-text-before-editing [editor-mode text]
  (condp = editor-mode
    ;:keyword (strip-colon text)
    text))

(defn is-inline-editor-modified? [editor-id]
  (let [inline-editor (get-atom-inline-editor-instance editor-id)
        raw-text (.getText inline-editor)]
    (or (empty? raw-text) (not= raw-text *initial-text*)))) ; empty editor is a special case of placeholder which has to removed

(defn get-inline-editor-mode [editor-id]
  {:post [(contains? known-editor-modes %)]}
  (let [$inline-editor-view ($ (get-atom-inline-editor-view-instance editor-id))]
    (class-name-to-editor-mode (some #(if (.hasClass $inline-editor-view %) %) known-editor-modes-classes))))

(defn get-value-after-editing [editor-id]
  (let [inline-editor (get-atom-inline-editor-instance editor-id)
        editor-mode (get-inline-editor-mode editor-id)
        raw-text (.getText inline-editor)]
    [editor-mode raw-text]))

(defn is-inline-editor-empty? [editor-id]
  (let [inline-editor (get-atom-inline-editor-instance editor-id)]
    (.isEmpty inline-editor)))

(defn dispatch-command-in-inline-editor [editor-id command]
  (let [inline-editor-view (get-atom-inline-editor-view-instance editor-id)]
    (.dispatch (.-commands atom) inline-editor-view command)))

(defn insert-text-into-inline-editor [editor-id text]
  (let [inline-editor (get-atom-inline-editor-instance editor-id)]
    (.insertText inline-editor text)))

(defn set-editor-mode-as-class-name-and-clear-text [_editor-id inline-editor inline-editor-view mode]
  (let [has-mode? (.hasClass ($ inline-editor-view) (editor-mode-to-class-name mode))]
    (when-not has-mode?
      (set-editor-mode-as-class-name inline-editor-view mode)
      (.setText inline-editor ""))))

(defn on-did-change [editor-id inline-editor inline-editor-view]
  (if (.isEmpty inline-editor)
    (.addClass ($ inline-editor-view) "empty")
    (.removeClass ($ inline-editor-view) "empty"))
  (let [text (.getText inline-editor)
        switch-mode (partial set-editor-mode-as-class-name-and-clear-text editor-id inline-editor inline-editor-view)]
    (condp = text
      ":" (switch-mode :keyword)
      "\"" (switch-mode :string)
      "'" (switch-mode :symbol)
      nil)))

(defn intial-inline-editor-setup-if-needed [editor-id inline-editor inline-editor-view]
  (if-not (get *on-did-change-disposables* editor-id)
    (set! *on-did-change-disposables*
      (assoc *on-did-change-disposables* editor-id (.onDidChange inline-editor (partial on-did-change editor-id inline-editor inline-editor-view))))))

(defn setup-inline-editor-for-editing [editor-id editor-mode text]
  {:pre [(contains? known-editor-modes editor-mode)]}
  (let [inline-editor (get-atom-inline-editor-instance editor-id)
        inline-editor-view (get-atom-inline-editor-view-instance editor-id)
        initial-text (preprocess-text-before-editing editor-mode text)]
    (intial-inline-editor-setup-if-needed editor-id inline-editor inline-editor-view)
    ; synchronous updates prevent intermittent jumps
    ; it has correct dimentsions before it gets appended in the DOM
    (.setUpdatedSynchronously inline-editor-view true)
    (set-editor-mode-as-class-name inline-editor-view editor-mode)

    (.setText inline-editor initial-text)
    (set! *initial-text* initial-text)
    (.selectAll inline-editor)
    (.setUpdatedSynchronously inline-editor-view false)))
(ns quark.cogs.editor.selection.model
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defmulti op (fn [op-type & _] op-type))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :move-down [_ cursel form-info]
  (let [sel-node-id (first cursel)]
    (log "want move down from" sel-node-id form-info)
    nil))

(defmethod op :move-up [_ cursel form-info]
  (let [sel-node-id (first cursel)]
    (log "want move up from" sel-node-id form-info)
    nil))

(defmethod op :move-right [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-tokens all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        line-tokens (get lines-tokens sel-line)
        _ (assert line-tokens)
        right-sibliks (rest (drop-while #(not= (:id %) sel-node-id) line-tokens))
        result (first right-sibliks)]
    (log "want move rigth from" sel-node-id result line-tokens)
    (if result
      #{(:id result)})))

(defmethod op :move-left [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-tokens all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        line-tokens (get lines-tokens sel-line)
        _ (assert line-tokens)
        left-sibliks (take-while #(not= (:id %) sel-node-id) line-tokens)
        result (last left-sibliks)]
    (log "want move left from" sel-node-id result line-tokens)
    (if result
      #{(:id result)})))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :default [command]
  (error (str "Unknown selection operation '" command "'"))
  nil)
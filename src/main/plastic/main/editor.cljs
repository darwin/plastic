(ns plastic.main.editor
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.editor.toolkit]
            [plastic.main.editor.model]
            [plastic.main.editor.lifecycle]
            [plastic.main.editor.loader]
            [plastic.main.editor.layout]
            [plastic.main.editor.analysis]
            [plastic.main.editor.ops]
            [plastic.main.editor.render]
            [plastic.main.editor.selection]
            [plastic.main.editor.xform]
            [plastic.main.editor.cursor]))

; -------------------------------------------------------------------------------------------------------------------

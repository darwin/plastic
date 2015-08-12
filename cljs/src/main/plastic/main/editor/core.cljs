(ns plastic.main.editor.core
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.editor.toolkit.core]
            [plastic.main.editor.lifecycle]
            [plastic.main.editor.watcher]
            [plastic.main.editor.model]
            [plastic.main.editor.loader]
            [plastic.main.editor.layout]
            [plastic.main.editor.analysis]
            [plastic.main.editor.ops]
            [plastic.main.editor.render.core]
            [plastic.main.editor.selections]))
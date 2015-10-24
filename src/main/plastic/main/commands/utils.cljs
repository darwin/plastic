(ns plastic.main.commands.utils
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

; -------------------------------------------------------------------------------------------------------------------

(defn toggle-setting [db key]
  (update-in db [:settings key] not))
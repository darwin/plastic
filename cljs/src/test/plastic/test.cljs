(ns plastic.test
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  [:require [cljs.test :refer-macros [run-all-tests run-tests]]
            [plastic.test.karma :as karma]
            [plastic.suites.all-tests]])

(aset js/__karma__ "start" karma/run-all-tests!)
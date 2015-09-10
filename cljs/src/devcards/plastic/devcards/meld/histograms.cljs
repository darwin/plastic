(ns plastic.devcards.meld.histograms
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [devcards.core :as dc :refer [defcard deftest]]
                   [plastic.devcards :refer [defmeldcard defhistcard]])
  (:require [meld.parser :as parser]
            [reagent.core :as r]
            [meld.support :refer [histogram-display]]
            [meld.core :as meld]))

(defhistcard symbol "symbol" false)
(defhistcard keyword ":keword" false)

(defhistcard list "(1 2)" false)
(defhistcard nested "[a b (1 2) 3]" true)
(defhistcard one "  1  " false)
(defhistcard comment "  ;comment  " false)
(defhistcard x-comment "  x ;comment  " false)


(defhistcard some-doc-example "(ns n1.doc)

; independent comment
; block spanning
; 3 lines


; -----------------------
; under separator

(defn log-ex
  \"Middleware which catches and prints any handler-generated exceptions to console.
  Handlers are called from within a core.async go-loop, and core.async produces
  a special kind of hell when in comes to stacktraces. By the time an exception
  has passed through a go-loop its stack is mangled beyond repair and you'll
  have no idea where the exception was thrown.
  So this middleware catches and prints to stacktrace before the core.async sausage
  machine has done its work.
  \"
  [handler]
  (fn log-ex-handler
    [db v] ; looooooooooooong comment sdaadjasidiasodjaosd saijdoiad joadj asdj iosjdas ijd iosajdoasjd asj
    (warn \"re-frame: use of \"log-ex\" is deprecated.\")

    (try
      ; inlined comment
      (handler db v)
      (catch :default e                                     ;; ooops, handler threw
        (do
          (.error js/console (.-stack e))  ; side-comment
          ; spanning multiple lines
          (throw e)))))) ; more

; comment after
; in block

; another comment

(def test \"some string\") ; test comment
" false)

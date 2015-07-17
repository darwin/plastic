(ns n1.onchanges)

(def on-changes
  "Middleware factory which acts a bit like \"reaction\"  (but it flows into db , rather than out)
  It observes N  inputs (paths into db) and if any of them change (as a result of the
  handler being run) then it runs 'f' to compute a new value, which is
  then assoced into the given out-path within app-db.

  Usage:

  (defn my-f
    [a-val b-val]
    ... some computation on a and b in here)

  (on-changes my-f [:c]  [:a] [:b])

  Put the middlware above on the right handlers (ones which might change :a or :b).
  It will:
     - call 'f' each time the value at path [:a] or [:b] changes
     - call 'f' with the values extracted from [:a] [:b]
     - assoc the return value from 'f' into the path  [:c]
  "
  ^{:re-frame-factory-name "on-changes"}
  (fn on-changes
    [f out-path & in-paths]
    (fn on-changed-middleware
      [handler]
      (fn on-changed-handler
        [db v]
        (let [;; run the handler, computing a new generation of db
              new-db (handler db v)

              ;; work out if any "inputs" have changed
              new-ins (map #(get-in new-db %) in-paths)
              old-ins (map #(get-in db %) in-paths)
              changed-ins? (some false? (map identical? new-ins old-ins))]

          ;; if one of the inputs has changed, then run 'f'
          (if changed-ins?
            (assoc-in new-db out-path (apply f new-ins))
            new-db))))))
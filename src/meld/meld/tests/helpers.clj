(ns meld.tests.helpers)

(defmacro subtree-tags [loc]
  `(map meld.zip/get-tag (meld.zip/take-subtree ~loc)))

(defmacro meld-from-source [source]
  `(meld.parser/parse! ~source))

(defmacro tags-from-source [source]
  `(subtree-tags (meld.zip/zip (meld-from-source ~source))))

(defmacro simple-meld-test [vname source tags]
  `(let [source# ~source]
     (plastic.devcards.util/meld-zip-card ~vname (plastic.devcards.util/markdown-source source#)
       (fn []
         (zip-from-source source#)))
     (plastic.devcards.util/deftest ~(symbol (str vname "-test"))
       (cljs.test/is (= (tags-from-source ~source) ~tags)))))

(defmacro simple-zip-test [vname zip-fn & tests]
  `(do
     (plastic.devcards.util/meld-zip-card ~vname nil ~zip-fn)
     (plastic.devcards.util/deftest ~(symbol (str vname "-test"))
       (let [~'selected-loc (~zip-fn)]
         ~@tests))))
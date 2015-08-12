(ns n1.tables)

(defn render-tree-component []
  (fn [render-tree]
    (let [{:keys [id tag children selectable?]} render-tree]
      [:div {:data-qnid id
             :class     (classv
                          (name tag)
                          (if selectable? "selectable"))}
       (condp = tag
         :tree (for [child children]
                 ^{:key (or (:id child) (name (:tag child)))}
                 [render-tree-component child])
         :code [code-box-component render-tree]
         :docs [docs-component render-tree]
         :headers [headers-wrapper-component render-tree]
         (throw (str "don't know how to render tag " tag " (missing render component implementation)")))])))
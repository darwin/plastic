(ns meld.comments
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

(defn strip-semicolon-and-whitespace [text]
  (.replace text #"^[;]+[\s\t]?" ""))

(defn merge-comment-tokens [tokens]
  (let [join-comments-content (fn [accum token]
                                (if-not (= :whitespace (:type token))
                                  (let [token-content (strip-semicolon-and-whitespace (:source token))
                                        existing-content (or
                                                           (:content accum)
                                                           (strip-semicolon-and-whitespace (:source accum)))]
                                    (str existing-content token-content))
                                  (:content accum)))
        merger (fn [accum token]
                 (-> accum
                   (assoc :end (:end token))
                   (assoc :line (:line token))
                   (assoc :content (join-comments-content accum token))
                   (update :source (fn [source] (str source (:source token))))))]
    (reduce merger tokens)))

(defn stitch-aligned-comments [tokens]
  (let [aligned? (fn [token1 token2]
                   (and
                     (= (:line token1) (dec (:line token2)))                                                          ; consequent lines
                     (= (:column token1) (:column token2))))                                                          ; and same columns
        stitching (fn [accum token]
                    (if (= (:type token) :comment)
                      (if (aligned? (:prev-comment accum) token)
                        (let [merged-token (merge-comment-tokens (persistent! (conj! (:backlog accum) token)))]
                          (assoc! accum :prev-comment merged-token)
                          (assoc! accum :backlog (transient [merged-token])))
                        (do
                          (assoc! accum :prev-comment token)
                          (assoc! accum :tokens (concat (:tokens accum) (persistent! (:backlog accum))))
                          (assoc! accum :backlog (transient [token]))))
                      (conj! (:backlog accum) token))
                    accum)
        result (reduce stitching (transient {:backlog (transient []) :tokens []}) tokens)]
    (concat (:tokens result) (persistent! (:backlog result)))))

(ns meld.comments
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.util :refer [update!]]))

(defn strip-semicolon-and-whitespace [text]
  (.replace text #"^[;]+[\s\t]?" ""))

(defn comment? [token]
  (keyword-identical? (:type token) :comment))

(defn linebreak? [token]
  (keyword-identical? (:type token) :linebreak))

(defn merge-comment-tokens [tokens]
  (if (and (= (count tokens) 1) (not (comment? (first tokens))))
    (first tokens)
    (let [join-comments-content (fn [accum token]
                                  (if (or (comment? token) (linebreak? token))
                                    (str (:content accum) (strip-semicolon-and-whitespace (:source token)))
                                    (:content accum)))
          merger (fn [accum token]
                   (-> accum
                     (assoc
                       :end (:end token)
                       :line (:line token)
                       :content (join-comments-content accum token))
                     (update :source (fn [source] (str source (:source token))))))
          first-token (assoc (first tokens)
                        :content (strip-semicolon-and-whitespace (:source (first tokens))))]
      (reduce merger first-token (rest tokens)))))

(defn look-ahead-if-comments-are-aligned [tokens comment]
  (let [aligned-comments? (fn [token1 token2]
                            (and
                              (= (:line token1) (dec (:line token2)))                                                 ; consequent lines
                              (= (:column token1) (:column token2))))]                                                ; and same columns
    (if-let [next-comment (some #(if (comment? %) %) tokens)]
      (aligned-comments? comment next-comment))))

(defn partition-aligned-comments [tokens]
  (loop [todo tokens
         prev-comment nil
         partitions []]
    (if (empty? todo)
      partitions
      (let [token (first todo)
            new-prev-comment (if (comment? token) token prev-comment)
            new-partitions (if (look-ahead-if-comments-are-aligned todo prev-comment)
                             (conj (pop partitions) (conj (last partitions) token))
                             (conj partitions [token]))]
        (recur (rest todo) new-prev-comment new-partitions)))))

(defn stitch-aligned-comments [tokens]
  (let [partitions (partition-aligned-comments tokens)]
    (map merge-comment-tokens partitions)))

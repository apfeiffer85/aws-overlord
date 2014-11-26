(ns aws-overlord.net
  (:require [clojure.string :refer [split join]]))

(defn- exp [x n]
  (apply * (repeat n x)))

(defn- shift [num to]
  (bit-shift-right (bit-and num (bit-shift-left 0xff to)) to))

(defn- ip->num [[a b c d]]
  (+ (* a (exp 256 3))
     (* b (exp 256 2))
     (* c 256)
     d))

(defn- num->ip [num]
  [(shift num 24)
   (shift num 16)
   (shift num 8)
   (bit-and num 0xFF)])

(defn- parse-ip [s]
  (ip->num (mapv #(Integer/parseInt %) (split s #"\."))))

(defn- parse-mask [s]
  (bit-shift-left -1 (- 32 (Integer/parseInt s))))

(defn- cidr-range [[network mask]]
  (let [low (bit-and network mask)
        high (+ low (bit-not mask))]
    [low high]))

(defn parse-cidr [cidr]
  (let [parts (split cidr #"/")
        network (-> parts first parse-ip)
        mask (-> parts second parse-mask)
        low (first (cidr-range [network mask]))]
    [low mask]))

(defn render-cidr
  ([[network mask]]
    (render-cidr network mask))
  ([network mask]
    (str (join "." (num->ip network)) "/" (Integer/bitCount mask))))

(defn split-cidr [cidr bits]
  (let [[network original-mask :as cidr] (parse-cidr cidr)
        new-mask (bit-shift-right original-mask bits)
        [low high] (cidr-range cidr)
        max (exp 2 bits)
        step (bit-and (bit-not original-mask) (/ (+ 1 (- high low)) max))]
    (mapv render-cidr (for [i (range max)]
                        [(+ network (* i step)) new-mask]))))

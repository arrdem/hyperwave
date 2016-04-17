(ns hyperwave.spammer
  "A module for generating spam message"
  {:author ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [clojure.java.io :as io])
  (:import java.io.File
           java.lang.StringBuilder))

(defonce words
  (->> (for [f     (.listFiles (io/file "/usr/share/dict"))
             :when (.isFile ^File f)
             word  (line-seq (io/reader f))]
         word)
       shuffle
       (take 1000)
       (into #{})))

(defn make-post [& {:keys [length] :or {length 140}}]
  (let [words (seq words)]
    (loop [^StringBuilder acc (StringBuilder.)]
      (let [word (rand-nth words)
            l    (.length acc)
            wl   (.length ^String word)]
        (if (<= (+ l 1 wl) length)
          (do (if-not (zero? l)
                (.append acc \space))
              (.append acc word)
              (recur acc))
          (.toString acc))))))

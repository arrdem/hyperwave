(ns hyperwave.web.backend
  (:require [taoensso.carmine :as car :refer [atomic wcar]]))

(def head "hyperwave:head")

(defn get-one [redis id]
  (let [[body next]
        (wcar redis
          (car/get (str id ":body"))
          (car/get id))]
    (when body
      {:ar (assoc body :id id)
       :dr next})))

(defn- feed* [redis id]
  (let [{:keys [ar dr]} (get-one redis id)]
    (when ar (lazy-seq (cons ar (when dr (feed* redis dr)))))))

(defn feed [redis]
  (feed* redis (wcar redis (car/get head))))

(defn put! [redis msg]
  (let [id  (str "hyperwave:" (java.util.UUID/randomUUID))
        bid (str id ":body")
        msg (assoc msg :date (java.util.Date.))]
    (and (atomic redis 100
                 (car/watch head)
                 (let [head* (car/with-replies (car/get head))]
                   (car/multi)
                   (car/set bid msg)
                   (car/set id head*)
                   (car/set head id)))
         (assoc msg :id id))))

(ns hyperwave.web.backend
  (:require [taoensso.carmine
             :as car
             :refer [wcar atomic]]
            [hyperwave.web.config :refer [*redis-conn*]]))

(def head "hyperwave:head")

(def *r
  {:pool {},
   :spec {:host "localhost",
          :port 6379}})

(defn get-one [id]
  (let [[body next]
        (wcar *redis-conn*
              (car/get (str id ":body"))
              (car/get id))]
    (when body (zipmap [:body :next] [body next]))))

(defn- feed* [id]
  (let [{:keys [body next]} (get-one id)]
    (when body
      (lazy-seq
       (cons (assoc body :id id)
             (when next (feed* next)))))))

(defn feed []
  (feed* (wcar *redis-conn* (car/get head))))

(defn put! [msg]
  (let [id  (str "hyperwave:" (java.util.UUID/randomUUID))
        bid (str id ":body")
        msg (assoc msg :date (java.util.Date.))]
    (and (atomic *redis-conn* 100
                 (car/watch head)
                 (let [head* (car/with-replies (car/get head))]
                   (car/multi)
                   (car/set bid msg)
                   (car/set id head*)
                   (car/set head id)))
         (assoc msg :id id))))

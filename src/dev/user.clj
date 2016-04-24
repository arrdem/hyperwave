(ns user
  (:require [bouncer.core :as bnc :refer [validate]]
            [hyperwave.web
             [backend :as b]
             [server :as server]]
            [interval-metrics.measure :refer [measure-latency periodically]]
            [taoensso.carmine :as car :refer [atomic wcar]]))

(defn start! []
  (and (server/start!)
       :ok))

(defn stop! []
  (and (server/stop!)
       :ok))

(defn restart! []
  (and (server/restart!)
       :ok))

(def *r
  {:pool {}
   :spec {:host "localhost"
          :port 6379}})

(defn post! [m]
  (b/put! *r m))

;; indentation crap
(alter-meta! #'measure-latency
             assoc :style/indent 1)

(alter-meta! #'validate
             assoc :style/indent 1)

(alter-meta! #'periodically
             assoc :style/indent 1)

(alter-meta! #'atomic
             assoc :style/indent 1)

(alter-meta! #'wcar
             assoc :style/indent 1)

(alter-meta! #'wcar
             assoc :style/indent 1)

(def *r
  {:pool {}
   :spec {:host "localhost"
          :port 6379}})

(defmacro in-thread [& forms]
  `(do (.start (java.lang.Thread. (fn [] ~@forms)))
       nil))

(defn bench! [workers interval]
  (let [state   (atom false)
        counter (java.util.concurrent.atomic.AtomicLong. 0)]
    (dotimes [i workers]
      (in-thread
       (loop []
         (do (try (b/put! *r {:body i :author (str "@speedbot:" i)})
                  (.getAndIncrement counter)
                  (catch Exception e nil))
             (when-not @state
               (recur))))))
    (Thread/sleep interval)
    (swap! state not)
    (.longValue counter)))

(defn do-bench! []
  (doseq [workers (range 10 500 10)
          time    (range 1000 6000 1000)]
    (let [res  (bench! workers time)
          line (str workers \tab time \tab res)]
      (println line)
      (spit "datafile.dat"
            (str line \newline)
            :append true))))

:ok

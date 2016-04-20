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

:ok

(ns user
  (:require [hyperwave.web.server :as server]
            [hyperwave.web.backend :as b]
            [hyperwave.web.routes :as r]))

(defn start! []
  (server/start!)
  :ok)

(defn stop! []
  (server/stop!)
  :ok)

(defn restart! []
  (server/restart!))

(def *r
  {:pool {},
   :spec {:host "localhost",
          :port 6379}})

(defn post! [m]
  (b/put! *r m))

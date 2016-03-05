(ns user
  (:require [hyperwave.web.server :as server]
            [rethinkdb.query :as r]))

(defn start! []
  (server/start!)
  :ok)

(defn stop! []
  (server/stop!)
  :ok)

(defn restart! []
  (server/restart!))

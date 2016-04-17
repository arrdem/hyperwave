(ns user
  (:require [hyperwave.web.server :as server]))

(defn start! []
  (server/start!)
  :ok)

(defn stop! []
  (server/stop!)
  :ok)

(defn restart! []
  (server/restart!))

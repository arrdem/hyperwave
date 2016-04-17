(ns user
  (:require [hyperwave.web.server :as server]
            [hyperwave.web.backend :as b]
            [hyperwave.web.routes :as r]
            [hyperwave.web.config :as cfg]))

(defn start! []
  (server/start!)
  :ok)

(defn stop! []
  (server/stop!)
  :ok)

(defn restart! []
  (server/restart!))

(defn post! [m]
  (binding [cfg/*redis-conn* b/*r]
    (r/app
     {:uri            "/api/v0/p"
      :request-method :post
      :form-params    m})))

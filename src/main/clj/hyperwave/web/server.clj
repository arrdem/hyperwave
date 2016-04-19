(ns hyperwave.web.server
  "Implementation of the runnable webserver

  Ties together the Ring routes with initializer code and a webserver."
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [compojure.handler :as handler]
            [ring.middleware.session :as session]
            [hyperwave.web.routes :refer [app]]
            [ring.adapter.jetty :as jetty]
            [interval-metrics.core :refer [snapshot! rate+latency rate]]
            [interval-metrics.measure :refer [periodically]]
            [taoensso.timbre :as timbre :refer [info warn]]))

(defn update-vals [f m]
  (into (empty m)
        (for [[k v] m]
          [k (f v)])))

(alter-meta! #'periodically
             assoc :style/indent 1)

(defonce
  ^{:doc "Keys
  - :jetty
  - :poller
  - :cfg"}
  -inst-
  (atom nil))

(defn running? []
  (not (nil? @-inst-)))

(defn stop! []
  (locking -inst-
    (let [{:keys [jetty poller]} @-inst-]
      (when jetty (.stop jetty))
      (when poller (poller)))
    (reset! -inst- nil)))

(defn start! [& [port? file?]]
  (locking -inst-
    (when-not @-inst-
      (let [jetty-cfg          {:host  "0.0.0.0"
                                :port  (or port? 3000)
                                :join? false}
            redis-cfg          {:pool {}
                                :spec {:host "localhost"
                                       :port 6379}}
            counters           {:head   (rate+latency)
                                :read   (rate+latency)
                                :insert (rate+latency)
                                :tfail  (rate)}
            sample             (atom nil)
            {:as       cfg
             jetty-cfg :jetty} {:redis       redis-cfg
                                :jetty       jetty-cfg
                                :counters    counters
                                :sample-atom sample}
            jetty-inst         (-> (app cfg)
                                   handler/site
                                   session/wrap-session
                                   (jetty/run-jetty jetty-cfg))
            poller-inst        (periodically 10 ; sec
                                 (reset! sample
                                         (-> (update-vals snapshot! counters)
                                             (assoc :period t))))]
        (info (format "Starting server: http://%s:%d" host port))
        (reset! -inst-
                {:jetty  jetty-inst
                 :poller poller-inst
                 :cfg    cfg})

        cfg))))

(defn restart! [& [port?]]
  (stop!)
  (start! port?)

  ;; Return nil b/c side-effects
  nil)

(defn -main [& [port?]]
  ;; Boot the webserver
  (start!
   (if (string? port?)
     (Long. port?)
     3000))

  ;; Return nil b/c side-effects
  nil)

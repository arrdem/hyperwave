(ns hyperwave.web.server
  "Implementation of the runnable webserver
  
  Ties together the Ring routes with initializer code and a webserver."
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [compojure.handler :as handler]
            [ring.middleware.session :as session]
            [hyperwave.web.routes :refer [app]]
            [ring.adapter.jetty :as jetty]
            [hyperwave.web.config :as cfg]
            [interval-metrics.core :refer [snapshot! rate+latency]]
            [interval-metrics.measure :refer [periodically]]
            [taoensso.timbre :as timbre :refer [info warn]]))

(alter-meta! #'periodically
             assoc :style/indent 1)

(defonce -inst-
  (atom nil))

(defonce -poller-
  (atom nil))

(defn stop! []
  (locking -inst-
    (locking -poller-
      (when-let [i @-inst-]
        (.stop i))
      (reset! -inst- nil)

      (when-let [p @-poller-]
        (p))
      (reset! -poller- nil))))

(defn start! [& [port? file?]]
  (locking -inst-
    (when-not @-inst-
      (let [host      "0.0.0.0"
            port      (or port? 3000)
            t         10 ; seconds, polling rate
            jetty-cfg {:host  host
                       :port  port
                       :join? false}
            
            ;; FIXME: don't hardcode
            redis-cfg {:pool {}
                       :spec {:host "localhost"
                              :port 6379}}

            ;; shared global state, but closed over here
            r1 (rate+latency)
            r2 (rate+latency)
            r3 (rate+latency)
            
            f       (fn [& args]
                      (binding [cfg/*redis-conn*  redis-cfg
                                cfg/*jetty-conn*  jetty-cfg
                                cfg/*insert-rate* r1
                                cfg/*head-rate*   r2
                                cfg/*read-rate*   r3]
                        (apply app args)))
            inst    (-> f
                        handler/site
                        session/wrap-session
                        (jetty/run-jetty jetty-cfg))
            watcher (periodically t
                      (reset! cfg/last-sample
                              {:period t
                               :put    (snapshot! r1)
                               :head   (snapshot! r2)
                               :read   (snapshot! r3)}))]
        (info (format "Starting server: http://%s:%d" host port))
        (reset! -inst- inst)
        (reset! -poller- watcher)))))

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

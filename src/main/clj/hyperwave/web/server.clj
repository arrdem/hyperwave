(ns hyperwave.web.server
  "Implementation of the runnable webserver
  
  Ties together the Ring routes with initializer code and a webserver."
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [compojure.handler :as handler]
            [ring.middleware.session :as session]
            [hyperwave.web.routes :refer [app]]
            [ring.adapter.jetty :as jetty]
            [hyperwave.web.config :as cfg]
            [interval-metrics.core :refer [rate]]
            [taoensso.timbre :as timbre :refer [info warn]]))

(defonce -inst-
  (atom nil))

(defn stop! []
  (locking -inst-
    (when-let [i @-inst-]
      (.stop i))
    (reset! -inst- nil)))

(defn start! [& [port? file?]]
  (locking -inst-
    (when-not @-inst-
      (let [host      "0.0.0.0"
            port      (or port? 3000)
            jetty-cfg {:host  host
                       :port  port
                       :join? false}
            ;; FIXME: don't hardcode
            redis-cfg {:pool {}
                       :spec {:host "localhost"
                              :port 6379}}
            f         (fn [& args]
                        (binding [cfg/*redis-conn*  redis-cfg
                                  cfg/*jetty-conn*  jetty-cfg
                                  cfg/*insert-rate* (rate)
                                  cfg/*head-rate*   (rate)
                                  cfg/*read-rate*   (rate)]
                          (apply app args)))
            inst      (-> f
                          handler/site
                          session/wrap-session
                          (jetty/run-jetty jetty-cfg))]
        (info (format "Starting server: http://%s:%d" host port))
        (reset! -inst- inst)))))

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

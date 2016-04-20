(ns hyperwave.web.server
  "Implementation of the runnable webserver

  Ties together the Ring routes with initializer code and a webserver."
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [compojure.handler :as handler]
            [hyperwave.web.routes :refer [app]]
            [interval-metrics
             [core :refer [rate rate+latency snapshot!]]
             [measure :refer [periodically]]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.session :as session]
            [taoensso.timbre :as timbre :refer [info]]))

(defn update-vals [f m]
  (into (empty m)
        (for [[k v] m]
          [k (f v)])))

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
  (try (locking -inst-
         (let [{:keys [jetty poller]} @-inst-]
           (when jetty (.stop jetty))
           (when poller (poller)))
         (reset! -inst- nil))
       :ok
       (catch Exception e
         nil)))

(defn start! [& [port? file?]]
  (try (locking -inst-
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
                 t                  10 ; sec
                 poller-inst        (periodically t
                                      (reset! sample
                                              (-> (update-vals snapshot! counters)
                                                  (assoc :period t))))]
             (info (format "Starting server: http://%s:%d"
                           (:host jetty-cfg)
                           (:port jetty-cfg)))
             (reset! -inst-
                     {:jetty  jetty-inst
                      :poller poller-inst
                      :cfg    cfg})

             cfg)))
       (catch Exception e
         nil)))

(defn restart! [& [port?]]
  (and (stop!)
       (start! port?)
       :ok))

(defn -main [& [port?]]
  ;; Boot the webserver
  (and (start!
        (if (string? port?)
          (Long. port?)
          3000))
       :ok))

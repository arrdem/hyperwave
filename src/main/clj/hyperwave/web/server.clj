(ns hyperwave.web.server
  (:require [compojure.handler :as handler]
            [ring.middleware.session :as session]
            [hyperwave.web.routes :refer [secured-app]]
            [hyperwave.web.config :refer :all]
            [ring.adapter.jetty :as jetty]
            [rethinkdb.query :as r]
            [taoensso.timbre :as timbre :refer [info warn]]))

(defn stop! []
  (locking rethink-inst
    (locking jetty-inst
      (when-let [inst @jetty-inst]
        (.stop inst)
        (warn "Stopped Jetty instance...")
        (reset! jetty-inst nil))
      (when-let [inst @rethink-inst]
        (r/close inst)
        (warn "Closed RethinkDB conn...")
        (reset! rethink-inst nil))))
  nil)

;; fixme: should be a conditional initializer
(defn ensure-tables [conn db tables]
  (let [pre      (-> (r/db db)
                     (r/table-list)
                     (r/run conn)
                     set)
        worklist (remove pre tables)]
    (->> worklist
         (mapv #(-> (r/db db)
                    (r/table-create %)
                    (r/run conn)
                    :tables_created))
         (apply +)
         (= (count worklist)))))

(defn ensure-db [conn db]
  (let [dbs (-> (r/db-list)
                (r/run conn)
                set)]
    (if-not (dbs db)
      (-> (r/db-create db)
          (r/run conn))
      true)))

(defn build-db!
  "Builds the various database tables"
  [& [conn?]]
  (if-let [conn (or conn? @rethink-inst)]
    (and (and (ensure-db conn rethink-db)
              (do (info "RethinkDB dbs ensured...")
                  true))
         (and (ensure-tables conn rethink-db
                             [users-table
                              posts-table
                              feeds-table
                              blacklists-table])
              (do (info "RethinkDB tables ensured...")
                  true)))
    false))

(defn start! [& [port? file?]]
  (locking rethink-inst
    (locking jetty-inst
      (stop!)
      (let [port      (or port? jetty-port)
            jetty-cfg {:port  port
                       :host  jetty-host
                       :join? false}
            inst      (-> secured-app
                          handler/site
                          session/wrap-session
                          (jetty/run-jetty jetty-cfg))]
        (info (format "Starting server: http://%s:%d" jetty-host port))
        (reset! jetty-inst inst))
      (let [conn (r/connect :host rethink-host
                            :port rethink-port
                            :db   rethink-db)]
        (info "RethinkDB conn open...")
        (when (build-db! conn)
          (info "RethinkDB conn ready...")
          (reset! rethink-inst conn)))))
  nil)

(defn restart! [& [port?]]
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

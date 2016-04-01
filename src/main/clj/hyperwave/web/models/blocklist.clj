(ns hyperwave.web.models.blocklist
  ""
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [clj-uuid :as uuid]
            [clojure.set :as set]
            [rethinkdb.query :as r]
            [hyperwave.web.config :as cfg]))

(defn make-blocklist [{:keys [admins subscribers body]}]
  {:pre [(not-empty admins)]}
  (let [id (uuid/v4)
        bl {:id          id
            :admins      admins
            :subscribers (set/union (set admins) (set subscribers))
            :body        []}]
    (-> (r/db cfg/rethink-db)
        (r/table cfg/blacklists-table)
        (r/insert [bl])
        (r/run @cfg/rethink-inst))
    id))

(defn get-blocklist [bl-id]
  (-> (r/db cfg/rethink-db)
      (r/table cfg/blacklists-table)
      (r/get bl-id)
      (r/run @cfg/rethink-inst)))

(defn- add-subscriber [bl user]
  (update :subscribers (comp vec #(conj % user) set)))

(defn subscribe! [bl-id user]
  (when-let [l (get-blocklist bl-id)]
    (-> (r/db cfg/rethink-db)
        (r/table cfg/blacklists-table)
        (r/insert [(add-subscriber l user)])
        (r/run @cfg/rethink-inst))))

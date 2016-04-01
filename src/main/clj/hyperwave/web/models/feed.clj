(ns hyperwave.web.models.feed
  "Provides a wrapper for interacting with feed structures
  
  Feeds:
  - Have a naming UUID
  - Have a list of UUIDs naming owners/admins
  - Have a list of UUIDs naming subscribers
  - Have a string title
  - Have a list of filters
    - UUID naming a user
    - Keyword
    - Hashtag
  "
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [hyperwave.web.config :as cfg]
            [clj-uuid :as uuid]
            [rethinkdb.query :as r]))

(defn make-feed [{:keys [admins subscribers body]}]
  (let [id (uuid/v4)
        bl {:id     id
            :admins admins
            :body   []}]
    (-> (r/db cfg/rethink-db)
        (r/table cfg/feeds-table)
        (r/insert [bl])
        (r/run @cfg/rethink-inst))
    id))

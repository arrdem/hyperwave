(ns hyperwave.web.config
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [environ.core :refer [env]]))

(defonce jetty-inst
  (atom nil))

(defonce rethink-inst
  (atom nil))

(let [{:keys [host port]
       :or   {host "localhost"
              port 4000}}
      (:jetty env)]
  (def jetty-host host)
  (def jetty-port port))

(let [{:keys [db users posts feeds
              blacklists host port]
       :or   {db         "hyperwave"
              users      "users"
              posts      "posts"
              feeds      "feeds"
              blacklists "blacklists"
              host       "arrdem.com"
              port       28015}}
      (:rethink env)]
  (def rethink-db db)
  (def rethink-host host)
  (def rethink-port port)
  
  (def users-table users)
  (def posts-table posts)
  (def feeds-table feeds)
  (def blacklists-table blacklists))

(def site
  {:base-url            "/"
   :version             "0.0.0-SNAPSHOT"
   :google-analytics-id "FIXME"
   :year                "2016"
   :author              {:me     "http://arrdem.com"
                         :email  "me@arrdem.com"
                         :gittip "/funding"}
   :js                  #{}
   :css                 #{}})

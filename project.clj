(defproject hyperwave "0.0.0"

  :source-paths ["src/main/clj"]
  :test-paths   ["src/test/clj"]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/tools.analyzer.jvm "0.6.9"]
                 [org.clojure/core.cache "0.6.4"]
                 [joda-time "2.8.2"]
                 [clj-time "0.11.0"]
                 [danlentz/clj-uuid "0.1.6"]
                 [aleph "0.4.1-beta5"]
                 [cheshire "5.5.0"]
                 [me.arrdem/guten-tag "0.1.6"]
                 [environ "1.0.2"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [cheshire "5.5.0"]
                 [com.taoensso/timbre "4.2.1"]
                 [com.apa512/rethinkdb "0.14.5-SNAPSHOT"]
                 [com.cemerick/friend "0.2.1"]
                 [me.arrdem/guten-tag "0.1.6"]]

  :aliases {"serve" ["with-profile" "server" "run"]}

  :profiles {:server {:env  {:rethink {:host "localhost"
                                       :port 3000}
                             :jetty   {:host "arrdem.com"
                                       :port 28015
                                       :db   "hyperwave"}}
                      :main hyperwave.web.server}

             :dev    {:dependencies [[ring/ring-mock "0.3.0"]]
                      :source-paths ["src/dev"]
                      :main         user}

             :user   [:server :dev :arrdem]})

(defproject hyperwave "0.0.0"

  :source-paths ["src/main/clj"]
  :test-paths   ["src/test/clj"]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [cheshire "5.5.0"]
                 [com.taoensso/timbre "4.2.1"]
                 [com.taoensso/carmine "2.12.2"]
                 [environ "1.0.2"] 
                 [me.arrdem/guten-tag "0.1.6"]
                 [bouncer "1.0.0"]
                 [interval-metrics "1.0.0"]]

  :aliases {"serve" ["with-profile" "server" "run"]}

  :profiles {:server {:env  {:rethink {:host "arrdem.com"
                                       :port 28015
                                       :db   "hyperwave"}
                             :jetty   {:host "localhost"
                                       :port 3000}}
                      :main hyperwave.web.server}

             :dev {:dependencies [[ring/ring-mock "0.3.0"]]
                   :source-paths ["src/dev"]
                   :main         user}

             :user [:server :dev :arrdem]})

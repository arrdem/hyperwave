(ns hyperwave.web.routes
  "Routes for the main hyperwave app

  This includes the basic HTML user interface, the admin console and the API"
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [bouncer
             [core :as bnc :refer [validate]]
             [validators :as v]]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [compojure.core :refer [context GET POST routes]]
            [hyperwave.web
             [backend :as b]
             [config :as cfg]]
            [interval-metrics
             [core :refer [update!]]
             [measure :refer [measure-latency]]]
            [ring.middleware.resource :refer [wrap-resource]])
  (:import clojure.lang.ExceptionInfo))

;; CIDER indentation crud
(alter-meta! #'measure-latency
             assoc :style/indent 1)

(alter-meta! #'validate
             assoc :style/indent 1)

(def app
  (-> (routes
       (GET "/" []
         {:status  200
          :headers {"Content-Type" "text/html"}
          :body    (slurp (io/resource "static/index.html"))})

       (GET "/api" []
         {:status  200
          :headers {"Content-Type" "text/plain"}
          :body    (str "The Hyperwave API has the following Routes:\n"
                        "  GET /api/v0/stats\n"
                        "  GET /api/v0/p\n"
                        "  POST /api/v0/p supported params: author=, body=, reply_to=\n"
                        "  GET /api/v0/p/:id")})

       (context "/api/v0" []
         (GET "/stats" []
           {:status  200
            :headers {"Content-Type" "text/plain"}
            :body    (json/encode @cfg/last-sample)})
         
         (GET "/p" {{limit :limit} :params}
           (measure-latency cfg/*head-rate*
             {:status  200
              :headers {"Content-Type" "text/plain"}
              :body    (json/encode {:status "OK"
                                     :body   (take (or (when limit (Long/parseLong limit)) 64)
                                                   (b/feed))})}))

         (GET "/p/:id" [id]
           (measure-latency cfg/*read-rate*
             (if-let [p (b/get-one id)]
               {:status  200
                :headers {"Content-Type" "text/plain"}
                :body    (json/encode {:status "OK" :body p})}
               {:status  404
                :headers {"Content-Type" "text/plain"}
                :body    (json/encode {:status "FAILURE" :body ["No such post"]})})))

         (POST "/p" {f :form-params
                     m :multipart-params}
           (measure-latency cfg/*insert-rate*
             (let [p       (select-keys (merge m f) ["author" "body" "reply_to"])
                   [_ res] (validate p
                             "author"  [[v/string
                                         :message "`author` may only be a string"]
                                        [v/matches #"^@[^\s]+$"
                                         :message "`author` must begin with @, cannot contain whitespace"]
                                        [v/max-count 64
                                         :message "`author` name may only be 64chrs long in total"]]
                             "body"    [[v/required
                                         :message "`body` is required"]
                                        [v/string
                                         :message "`body` may only be a string"]
                                        [v/max-count 1024
                                         :message "`body` is limited to 1024chrs in length"]]
                             "reply_to" [[v/string
                                          :message "`reply_to` must be a valid post ID if present"]
                                         [v/max-count 46
                                          :message "`reply_to` must be a valid post ID if present"]])]
               ;; FIXME: validate author, body, reply_to strs
               (cond (::bnc/errors res)
                     ,,{:status  500
                        :headers {"Content-Type" "text/plain"}
                        :body    (json/encode
                                  {:status "FAILURE"
                                   :body   (vec (vals (::bnc/errors res)))})}

                     (if-let [id (get p "reply_to")]
                       (not (b/get-one id)))
                     ,,{:status  500
                        :headers {"Content-Type" "text/plain"}
                        :body    (json/encode {:status "FAILURE"
                                               :body   ["`reply_to` must be a valid post ID if present"]})}

                     :else
                     ,,(try (let [b (b/put! p)]
                              {:status  200
                               :headers {"Content-Type" "text/plain"}
                               :body    (json/encode {:status "OK" :body b})})
                            (catch ExceptionInfo i
                              (update! cfg/*tfail-rate* 1)
                              {:status  503
                               :headers {"Content-Type" "text/plain"
                                         "Retry-After"  3}
                               :body    (json/encode
                                         {:status "FAILURE"
                                          :body   ["Failed to commit post due to write contention"]})}))))))))
      (wrap-resource "public")))

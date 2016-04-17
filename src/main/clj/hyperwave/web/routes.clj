(ns hyperwave.web.routes
  "Routes for the main hyperwave app

  This includes the basic HTML user interface, the admin console and the API"
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [clojure.string :as str]
            [compojure
             [core :refer [ANY context defroutes GET POST]]
             [route :as route]]
            [ring.util.response :as response]
            [hyperwave.web.backend :as b]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre :refer [info warn debug]]))

(defroutes app
  (GET "/" []
    {:status  200
     :headers {"Content-Type" "text/plain"}
     :body    (str "Welcome to Hyperwave\n"
                   "Routes:\n"
                   "  GET /api/v0/p\n"
                   "  POST /api/v0/p supported params: author=, body=, reply_to=\n"
                   "  GET /api/v0/p/:id")})

  (context "/api/v0" []
    (GET "/p" []
      {:status 200
       :body   (json/encode {:status "OK" :body (take 64 (b/feed))})})

    (GET "/p/:id" [id]
      (if-let [p (b/get-one id)]
        {:status 200
         :body   (json/encode p)}
        {:status 404
         :body   (json/encode {:status "FAILURE" :body ["No such post"]})}))

    (POST "/p" {p :params}
      (let [p (select-keys p ["author" "body" "reply_to"])]
        (cond (not p)
              ,,{:status 500
                 :body   (json/encode {:status "FAILURE"
                                       :body   ["No keys found, supported POST params are author, body, reply_to"]})}

              (some? #(< 1024 (count %)) (vals p))
              ,,{:status 500
                 :body   (json/encode {:status "FAILURE"
                                       :body   ["no val can be over 1024 bytes"]})}

              (not (when-let [id (get p "reply_to")]
                     (b/get-one id)))
              ,,{:status 500
                 :body   (json/encode {:status "FAILURE"
                                       :body   ["reply_to must be a valid post ID if supplied"]})}

              :else
              ,,(if-let [b (b/put! p)]
                  {:status 200
                   :body   (json/encode {:status "OK" :body p})}
                  {:status 500
                   :body   (json/encode {:status "FAILURE" :body ["Failed to write post"]})}))))))


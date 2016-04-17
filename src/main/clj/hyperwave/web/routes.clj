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
            [bouncer
             [core :refer [validate valid?] :as bnc]
             [validators :as v]]
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

    (POST "/p" {f :form-params
                m :multipart-params}
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
              ,,{:status 500
                 :body   (json/encode
                          {:status "FAILURE"
                           :body   (vec (vals (::bnc/errors res)))})}

              (if-let [id (get p "reply_to")]
                (not (b/get-one id)))
              ,,{:status 500
                 :body   (json/encode {:status "FAILURE"
                                       :body   ["`reply_to` must be a valid post ID if present"]})}

              :else
              ,,(if-let [b (b/put! p)]
                  {:status 200
                   :body   (json/encode {:status "OK" :body p})}
                  {:status 500
                   :body   (json/encode {:status "FAILURE" :body ["Failed to write post"]})}))))))

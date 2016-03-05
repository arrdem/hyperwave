(ns hyperwave.web.routes
  (:require [cemerick.friend :as friend]
            [cemerick.friend
             [credentials :as creds]
             [workflows :as workflows]]
            [compojure
             [core :refer [ANY context defroutes GET PUT]]
             [route :as route]]
            [ring.util.response :as response]
            [hyperwave.web.views.content.html :as html]
            [hyperwave.web.models.user :as m.u]
            [taoensso.timbre :as timbre :refer [warn]]))

(defroutes public-routes
  (GET "/" []
    "Public / page FIXME")

  (GET "/robots.txt" []
    (response/redirect "/public/robots.txt"))
  
  (GET "/login" []
    (html/login-view))

  (friend/logout
   (ANY "/logout" []
     (ring.util.response/redirect "/")))

  (GET "/signup" []
    (html/signup-view))

  (PUT "/signup" {{:keys [username password]} :params}
    )
  
  (route/resources "/public")

  (route/not-found
   (fn [{uri :uri :as req}]
     (warn (pr-str {:uri        uri
                    :type       :html
                    :user-agent (get-in req [:headers "user-agent"])}))
     "oshit 404 cri ^^( Q,,,Q)^^ sad cthulu")))

(defroutes user-routes
  (GET "/home" []
    (html/authed-home))
  
  (GET "/account" []
    (html/user-settings)))

(defroutes admin-routes
  (GET "/" []
    (html/admin-view))

  (GET "/account" []
    (html/admin-settings)))

(defroutes app-routes 
  (context "/user" []
    (friend/wrap-authorize user-routes #{::m.u/user}))

  ;; requires admin role
  (context "/admin" []
    (friend/wrap-authorize admin-routes #{::m.u/admin}))

  public-routes)

(defn update-cookie-counter [request]
  (-> request
      (update-in [:session :counter]          (fnil inc 0))
      (update-in [:session :thought-about-it] (fnil identity false))))

(defn app [request]
  (let [request (update-cookie-counter request)
        resp    (app-routes request)]
    (if-not (:session resp)
      (assoc resp :session (:session request))
      resp)))

(def secured-app
  (-> app
      (friend/authenticate
       {:credential-fn       (partial creds/bcrypt-credential-fn m.u/get-user)
        :workflows           [(workflows/interactive-form)]
        :default-landing-uri "/user/home"})))

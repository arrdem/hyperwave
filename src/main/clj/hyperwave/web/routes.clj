(ns hyperwave.web.routes
  "Routes for the main hyperwave app

  This includes the basic HTML user interface, the admin console and the API"
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [clojure.string :as str]
            [cemerick.friend :as friend]
            [cemerick.friend
             [credentials :as creds]
             [workflows :as workflows]]
            [compojure
             [core :refer [ANY context defroutes GET PUT]]
             [route :as route]]
            [ring.util.response :as response]
            [hyperwave.web.views.content.html :as html]
            [hyperwave.web.models.user :as m.u]
            [taoensso.timbre :as timbre :refer [info warn debug]]))

(defroutes public-routes
  (GET "/" []
    "Public / page FIXME")

  (GET "/robots.txt" []
    (response/redirect "/public/robots.txt"))
  
  (GET "/login" []
    (html/login-view))

  ;; Implictit PUT /login from friend

  (friend/logout
   (ANY "/logout" []
     (ring.util.response/redirect "/")))

  (GET "/signup" []
    (html/signup-view))
  
  (route/resources "/public")

  (route/not-found
   (fn [{uri :uri :as req}]
     (warn (pr-str {:uri        uri
                    :type       :html
                    :user-agent (get-in req [:headers "user-agent"])}))
     "oshit 404 cri ^^( T,,,T)^^ sad cthulu")))

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

(defn auth-fn [{:keys [username password] :as args}]
  (debug "Trying to log in:" args)
  (if-let [user (m.u/get-user username)]
    (if (creds/bcrypt-verify password (:password user))
      (do (info (format "User '%s' logged in!" username))
          user)
      (debug "User" username "didn't provide the correct password!"))
    (debug "Couldn't find user" username)))

(defn do-register [{:keys [username password confirm] :as params}]
  (if-let [errors (m.u/validate-user params)]
    (html/signup-view :errors errors)
    (workflows/make-auth (m.u/add-user! params))))

(defn registration-form [& {:keys [register-uri]}]
  (fn [{:keys [request-method params uri]}]
    (when (and (= uri register-uri)
               (= request-method :post))
      (do-register params))))

(def secured-app
  (-> app
      (friend/authenticate
       {:credential-fn       auth-fn
        :workflows           [(workflows/interactive-form :login-uri "/login")
                              (registration-form :register-uri "/signup")]
        :default-landing-uri "/user/home"
        :login-uri           "/login"})))

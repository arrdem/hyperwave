(ns hyperwave.web.views.content.html
  (:require [hyperwave.web.views.layout :refer [layout]]
            [hyperwave.web.config :as cfg]
            [hiccup.form :as form]))

(defn unauthed-home []
  (layout
   cfg/site
   "Unauthed user home T_T nothing here yet..."))

(defn signup-view [])

(defn login-view []
  (layout
   cfg/site
   (form/form-to
    [:put "/login"]
    "Username: " (form/text-field "username") [:br]
    "Password: " (form/text-field "password") [:br]
    (form/submit-button "Submit"))))

(defn authed-home []
  (layout
   cfg/site
   "Congrats you're logged in!"))

(defn user-view []
  (layout
   cfg/site
   "User view.... FIXME"))

(defn feed-view []
  (layout
   cfg/site
   "Feed view.... FIXME"))

(defn blacklist-view []
  (layout
   cfg/site
   "Blacklist view.... FIXME"))

(defn user-settings []
  (layout
   cfg/site
   "User settings.... FIXME"))

(defn admin-view []
  (layout
   cfg/site
   "Admin viewwwwww"))

(defn admin-settings []
  (layout
   cfg/site
   "Admin settings...."))

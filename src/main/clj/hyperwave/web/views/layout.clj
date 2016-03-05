(ns hyperwave.web.views.layout
  (:require [hiccup.page :as page]
            [hyperwave.web.config :as cfg]
            [hyperwave.util :refer [defnc]]))

(defn header [{:keys [base-url css] :as c}]
  [:head
   [:link {:rel "profile" :href "http://gmpg.org/xfn/11"}]
   [:meta {:content "IE=edge" :http-equiv "X-UA-Compatible"}]
   [:meta {:content "text/html; charset=utf-8" :http-equiv "content-type"}]
   [:meta {:content "width=device-width initial-scale=1.0 maximum-scale=1" :name "viewport"}]
   (when-let [pg (:page c)]
     (list
      (when-let [description (:description pg)]
        [:meta {:name    "description"
                :content description}])
      (when-let [summary (:summary pg)]
        [:meta {:name    "summary"
                :content summary}])))
   [:title (-> c :style :title)]
   (map page/include-css css)
   [:link {:href "https://fonts.googleapis.com/css?family=PT+Serif:400,400italic,700|PT+Sans:400"
           :rel  "stylesheet"}]])

(defnc ga [id]
  [:script {:type "text/javascript"}
   (str "var _gaq = _gaq || [];
  _gaq.push(['_setAccount', '" id "']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();")])

(defn footer [{:keys [google-analytics-id js]}]
  [:footer
   (map page/include-js js)

   ;; FIXME: factor this out
   (ga google-analytics-id)])

(defn layout
  [page & content]
  (page/html5
   (header page)
   [:body
    [:div.wrap
     [:div {:class "container content"}
      (if (:page page false)
        [:div {:class "page"}
         content]
        content)]]
    [:label.sidebar-toggle {:for "sidebar-checkbox"}]]
   (footer page)))

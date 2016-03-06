(ns hyperwave.web.models.user
  (:require [hyperwave.web.config :as cfg]
            [hyperwave.web.models.feed :as m.f]
            [hyperwave.web.models.blocklist :as m.b]
            [rethinkdb.query :as r]))

(defn- bcrypt-hash?
  "Validates that the given String is an encoded bcrypt hash."
  [s]
  {:pre [(string? s)]}
  (boolean (re-find #"\$2[aby]\$\d*\$[^\$]{53}" s)))

(derive ::admin ::user)

#_(def users
    {"root"     {:username "root"
                 :password (creds/hash-bcrypt "admin_password")
                 :roles    [::admin]}
     "arrdrunk" {:username "arrdrunk"
                 :password (creds/hash-bcrypt "user_password")
                 :roles    [::user]}})

(defn- ->user [{:keys [id password roles meta] :as u}]
  (when u (update u :roles (comp set (partial map keyword)))))

(defn get-user [username]
  (-> (r/db cfg/rethink-db)
      (r/table cfg/users-table)
      (r/get username)
      (r/run @cfg/rethink-inst)
      ->user))

(defn exists? [username]
  (boolean (get-user username)))

(defn add-user! [{:keys [username password] :as u}]
  {:pre [(string? username)
         (not (exists? username))
         (bcrypt-hash? password)]}
  (let [u' (-> u
               (assoc :id username)
               (dissoc :username)
               (update :roles seq)
               (update :feeds vec)
               (update :blocklists vec))]
    (-> (r/db cfg/rethink-db)
        (r/table cfg/users-table)
        (r/insert [u'])
        (r/run @cfg/rethink-inst))

    (let [feed      (m.f/make-feed      {:admins [username]})
          blocklist (m.b/make-blocklist {:admins [username]})
          u''       (-> u'
                        (update :feeds conj feed)
                        (update :blocklists conj blocklist))]
      ;; upsert
      (-> (r/db cfg/rethink-db)
          (r/table cfg/users-table)
          (r/get username)
          (r/insert [u''])
          (r/run @cfg/rethink-inst)))))

(defn get-users []
  (map ->user
       (-> (r/db cfg/rethink-db)
           (r/table cfg/users-table)
           (r/run @cfg/rethink-inst))))

(defn try-auth [username pw]
  {:pre [(string? username)
         (bcrypt-hash? pw)]}
  (or (when-let [u (get-user username)]
        (let [{:keys [password]} u]
          (if (= pw password)
            u)))
      false))

;; FIXME: needs username search

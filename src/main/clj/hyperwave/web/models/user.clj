(ns hyperwave.web.models.user
  (:require [hyperwave.web.config :as cfg]
            [hyperwave.web.models.feed :as m.f]
            [hyperwave.web.models.blocklist :as m.b]
            [rethinkdb.query :as r]))

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

(defn add-user! [{:keys [username ^String password] :as u}]
  {:pre [(string? username)
         (not (exists? username))
         (string? password)
         ;; FIXME: is this fragile or not?
         (.startsWith password "$2a$10$")]}
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
          blocklist (m.f/make-blocklist {:admins [username]})
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

(defn try-auth [username ^String pw]
  {:pre [(string? username)
         (string? pw)
         ;; FIXME: is this fragile or not?
         (.startsWith pw "$2a$10$")]}
  (or (when-let [u (get-user username)]
        (let [{:keys [password]} u]
          (if (= pw password)
            u)))
      false))

;; FIXME: needs username search

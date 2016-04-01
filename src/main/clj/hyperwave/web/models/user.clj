(ns hyperwave.web.models.user
  ""
  {:authors ["Reid 'arrdem' McKenzie <me@arrdem.com>"]}
  (:require [hyperwave.web.config :as cfg]
            [hyperwave.web.models.feed :as m.f]
            [hyperwave.web.models.blocklist :as m.b]
            [cemerick.friend.credentials :as creds]
            [clj-uuid :as uuid]
            [rethinkdb.query :as r]))

(defn- bcrypt-hash?
  "Validates that the given String is an encoded bcrypt hash."
  [s]
  {:pre [(string? s)]}
  (boolean (re-find #"\$2[aby]\$\d*\$[^\$]{53}" s)))

;; FIXME: users need an "enabled" state of some sort
;; - could be banned until some time
;; - could be disabled
;; - could be deleted

(defn- ->user [{:keys [id password roles meta blacklists feeds] :as u}] 
  (some-> u
          (update :roles (comp set (partial map keyword)))
          (update :id #(uuid/as-uuid %))
          (update :enabled #(java.lang.Boolean/parseBoolean %))))

;; FIXME: prime use for a LRU cache at least
(defn get-user
  "Looks up a user from the db by ID, returning either a user structure or nil if there is no user
  with that username."
  [username]
  (-> (r/db cfg/rethink-db)
      (r/table cfg/users-table)
      ;; FIXME: use an index?
      (r/filter (r/fn [user] (r/eq username (r/get-field user :username))))
      (r/run @cfg/rethink-inst)
      first
      ->user))

(defn exists?
  "Looks for a user in the DB by ID, returning true if such a user exists, otherwise nil."
  [username]
  (boolean (get-user username)))

(defn validate-user [{:keys [username password confirm email]}])

(defn add-user!
  "Inserts a new user into the database, creating a blocklist and a feed for them."
  [{:keys [username password] :as u}]
  {:pre [(string? username)
         (not (exists? username))
         (bcrypt-hash? password)]}
  (let [id (get u :id (uuid/v4))
        u' (-> u
               (assoc :id id)
               (update :roles (comp #(conj % ::user) set))
               (update :feeds vec)
               (update :blocklists vec))]
    (-> (r/db cfg/rethink-db)
        (r/table cfg/users-table)
        (r/insert [u'])
        (r/run @cfg/rethink-inst))

    (let [feed      (m.f/make-feed      {:admins [id]})
          blocklist (m.b/make-blocklist {:admins [id]})
          u''       (-> u'
                        (update :feeds conj feed)
                        (update :blocklists conj blocklist))]
      ;; upsert
      (-> (r/db cfg/rethink-db)
          (r/table cfg/users-table)
          (r/insert [u''])
          (r/run @cfg/rethink-inst))

      u'')))

(defn get-users
  "Returns a seq of all the users in the database as full records."
  []
  (map ->user
       (-> (r/db cfg/rethink-db)
           (r/table cfg/users-table)
           (r/run @cfg/rethink-inst))))

;; FIXME: needs username search

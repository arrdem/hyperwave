(ns hyperwave.web.backend
  (:require [taoensso.carmine :as car :refer [atomic wcar]])
  (:import java.lang.IllegalArgumentException))

(alter-meta! #'atomic
             assoc :style/indent 2)

(def *prefix "hyperwave:list:")

(defprotocol Listable
  (as-list [l]))

(deftype List [^String head ^String prefix]
  Listable
  (as-list [this] this))

(defn ->List [key]
  (if (.startsWith key *prefix)
    (List. key "hyperwave:")
    (throw
     (IllegalArgumentException.
      (format "Got a string without the key prefix! \"%s\"" key)))))

(extend-protocol Listable
  String
  (as-list [s] (->List s *prefix))

  Object
  (as-list [o]
    (throw
     (IllegalArgumentException.
      (format "as-list does not support (%s %s)" o (class o))))))

(defn- list->counter [^List l]
  {:pre [(instance? List l)]}
  (str (.head l) ":length"))

(defn- list->head [^List l]
  {:pre [(instance? List l)]}
  (str (.head l) ":head"))

(defn- list->tail [^List l]
  {:pre [(instance? List l)]}
  (str (.head l) ":tail"))

(defn mklist [conn name]
  (let [l         (->List (str "hyperwave:list:" name))
        count-key (list->counter l)
        head-key  (list->head l)
        tail-key  (list->tail l)]
    (atomic conn 100
      (car/watch head-key)
      (car/watch tail-key)
      (car/watch count-key)
      (car/multi)
      (when (= 0 (or (car/get count-key) 0))
        (car/set head-key nil)
        (car/set tail-key nil)
        (car/incr count-key)))
    l))

(defn- elem->body [e]
  (str e ":body"))

(defn- elem->next [e]
  e)

;; FIXME: cursors are an implementation detail and need a weird custom constructor.
(deftype Cursor [conn ^String head ^int count]
  clojure.lang.Counted
  (count ^int [_] count)

  clojure.lang.Seqable
  (seq [_]
    (let [[this nk :as res] (wcar conn
                              (car/get (elem->body head))
                              (car/get (elem->next head)))]
      (when this
        (lazy-seq
         (cons (assoc this :id head)
               (when nk
                 (Cursor. conn nk (dec count)))))))))

(defn list->cursor [conn l]
  (let [l                  (as-list l)
        ck                 (list->counter l)
        hk                 (list->head l)
        res                (atomic conn 10
                             (car/watch ck)
                             (car/watch hk)
                             (car/multi)
                             (car/get ck)
                             (car/get hk))
        [_ _ [count head]] res]
    (Cursor. conn head (int (or count 0)))))

(defn push! [conn l r]
  (let [^List l (as-list l)
        ck      (list->counter l)
        hk      (list->head l)
        prefix  (str (.prefix l) "lelem:")
        id      (str prefix (java.util.UUID/randomUUID))
        bk      (elem->body id)
        res     (atomic conn 1
                  (car/watch ck)
                  (car/watch hk)
                  (car/multi)
                  (let [head* (car/with-replies (car/get hk))]
                    (println "[push!]" head*)
                    (car/set bk r)
                    (car/set id head*)
                    (car/set hk id)
                    (car/incr ck)))]
    (println "[push!]" res)
    id))

(defn feed [conn]
  (let [l      (mklist conn "master")
        cursor (list->cursor conn l)]
    cursor))

(defn put! [conn msg]
  (let [l  (mklist conn "master")
        id (push! conn l msg)]
    (assoc msg :id id)))

(defn get-one [conn id]
  (let [[ar dr] (wcar conn
                  (car/get (elem->body id))
                  (car/get (elem->next id)))]
    (when ar
      {:ar (assoc ar :id id) :dr dr})))

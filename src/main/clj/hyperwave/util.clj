(ns hyperwave.util
  (:import java.lang.ref.SoftReference))

(defmacro defnc [name args & body]
  `(def ~name
     (let [cache# (atom (SoftReference. nil))]
       (fn ~args
         (or (.get ^SoftReference @cache#)
             (let [res# (do ~@body)]
               (reset! cache# (SoftReference. res#))
               res#))))))

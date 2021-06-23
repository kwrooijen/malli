(ns malli.impl.util
  #?(:clj (:import (java.util.concurrent TimeoutException TimeUnit FutureTask)
                   (clojure.lang MapEntry))
     :cljr (:import (clojure.lang MapEntry))))

(def ^:const +max-size+ #?(:clj Long/MAX_VALUE, :cljr Int64/MaxValue, :cljs (.-MAX_VALUE js/Number)))

(defn -tagged [k v] #?(:clj (MapEntry. k v)
                       :cljr (MapEntry. k v)
                       :cljs (MapEntry. k v nil)))
(defn -tagged? [v] (instance? MapEntry v))

(defn -invalid? [x] #?(:clj (identical? x :malli.core/invalid)
                       :cljr (identical? x :malli.core/invalid)
                       :cljs (keyword-identical? x :malli.core/invalid)))
(defn -map-valid [f v] (if (-invalid? v) v (f v)))
(defn -map-invalid [f v] (if (-invalid? v) (f v) v))

(defrecord SchemaError [path in schema value type message])

(defn -error
  ([path in schema value] (->SchemaError path in schema value nil nil))
  ([path in schema value type] (->SchemaError path in schema value type nil)))

#?(:clj
   (defn ^:no-doc -run [^Runnable f ms]
     (let [task (FutureTask. f), t (Thread. task)]
       (try
         (.start t) (.get task ms TimeUnit/MILLISECONDS)
         (catch TimeoutException _ (.cancel task true) (.stop t) ::timeout)
         (catch Exception e (.cancel task true) (.stop t) (throw e))))))

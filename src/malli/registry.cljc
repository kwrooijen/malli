(ns malli.registry
  (:refer-clojure :exclude [type]))

#?(:cljs (goog-define type "default")
   :clj  (def type (as-> (or (System/getProperty "malli.registry/type") "default") $ (.intern $)))
   :cljr  (def type (as-> (or (Environment/GetEnvironmentVariable "malli.registry/type") "default") $ (String/Intern $))))

(defprotocol Registry
  (-schema [this type] "returns the schema from a registry")
  (-schemas [this] "returns all schemas from a registry"))

(defn simple-registry [schemas]
  (reify
    Registry
    (-schema [_ type] (get schemas type))
    (-schemas [_] schemas)))

(defn registry [?registry]
  (cond (satisfies? Registry ?registry) ?registry
        (map? ?registry) (simple-registry ?registry)))

;;
;; custom
;;

(def ^:private registry* (atom (registry {})))

(defn set-default-registry! [?registry]
  (if (identical? type "custom")
    (reset! registry* (registry ?registry))
    (throw (ex-info "can't set default registry" {:type type}))))

(defn ^:no-doc custom-default-registry []
  (reify
    Registry
    (-schema [_ type] (-schema @registry* type))
    (-schemas [_] (-schemas @registry*))))

(defn composite-registry [& ?registries]
  (let [registries (mapv registry ?registries)]
    (reify
      Registry
      (-schema [_ type] (some #(-schema % type) registries))
      (-schemas [_] (reduce merge (map -schemas (reverse registries)))))))

(defn mutable-registry [db]
  (reify
    Registry
    (-schema [_ type] (-schema (registry @db) type))
    (-schemas [_] (-schemas (registry @db)))))

(def ^:dynamic *registry* {})

(defn dynamic-registry []
  (reify
    Registry
    (-schema [_ type] (-schema (registry *registry*) type))
    (-schemas [_] (-schemas (registry *registry*)))))

(defn lazy-registry [default-registry provider]
  (let [cache* (atom {})
        registry* (atom default-registry)]
    (reset!
      registry*
      (composite-registry
        default-registry
        (reify
          Registry
          (-schema [_ name]
            (or (@cache* name)
                (when-let [schema (provider name @registry*)]
                  (swap! cache* assoc name schema)
                  schema)))
          (-schemas [_] @cache*))))))

(defn schema
  "finds a schema from a registry"
  [registry type]
  (-schema registry type))

(defn schemas
  "finds all schemas from a registry"
  [registry]
  (-schemas registry))

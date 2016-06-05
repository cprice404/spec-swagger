(ns cprice404.swagger-ui-service
  (:require [puppetlabs.trapperkeeper.core :as tk]
            [ring.swagger.ui :as ring-swagger-ui]
            [ring.swagger.swagger2 :as rs]
            [schema.core :as schema]
            [ring.middleware.json :refer [wrap-json-response]]
            [clojure.spec :as spec]
            [clojure.set :as set]
            [clojure.string :as str]
            [ring.swagger.validator :as swagger-validator]))

(spec/instrument-ns 'cprice404.swagger-ui-service)

(defn swagger-ui-handler
  []
  (ring-swagger-ui/swagger-ui "/docs"))

(schema/defschema User {:id schema/Str,
                   :name schema/Str
                   :address {:street schema/Str
                             :city (schema/enum :tre :hki)}})

(defn swagger-json1
  []
  {:body (rs/swagger-json
          {:info {:version "1.0.0"
                  :title "Sausages"
                  :description "Sausage description"
                  :termsOfService "http://helloreverb.com/terms/"
                  :contact {:name "My API Team"
                            :email "foo@example.com"
                            :url "http://www.metosin.fi"}
                  :license {:name "Eclipse Public License"
                            :url "http://www.eclipse.org/legal/epl-v10.html"}}
           :tags [{:name "user"
                   :description "User stuff"}]
           :paths {"/api/ping" {:get {}}
                   "/user/:id" {:post {:summary "User Api"
                                       :description "User Api description"
                                       :tags ["user"]
                                       :parameters {:path {:id schema/Str}
                                                    :body User}
                                       :responses {200 {:schema User
                                                        :description "Found it!"}
                                                   404 {:description "Ohnoes."}}}}}})})

(defn swagger-json1-handler
  [req]
  (swagger-json1))

;(s/defschema Swagger
;  {(opt :info) Info
;   (opt :paths) {s/Str {s/Keyword (s/maybe Operation)}}
;   s/Keyword s/Any})
;
;(s/defschema Info
;             {X- s/Any
;              :title s/Str
;              (opt :version) s/Str
;              (opt :description) s/Str
;              (opt :termsOfService) s/Str
;              (opt :contact) {(opt :name) s/Str
;                              (opt :url) s/Str
;                              (opt :email) s/Str}
;              (opt :license) {:name s/Str
;                              (opt :url) s/Str}})

(defn boolean?
  [x]
  (instance? Boolean x))

(defn no-extra-keys?
  [expected-keys m]
  (empty? (set/difference (set (keys m)) (set expected-keys))))

(defn assoc-if-not-nil
  [m k v]
  (if v
    (assoc m k v)
    m))

(defn assoc-if-not-empty
  [m k v]
  (if (empty? v)
    m
    (assoc m k v)))



(spec/def :spec-swagger/swagger
  (spec/keys :opt-un [:spec-swagger/info :spec-swagger/paths]))

(spec/def :spec-swagger/info
  (spec/keys :req-un [:spec-swagger.info/title
                      :spec-swagger.info/version]
             :opt-un [:spec-swagger.info/description]))

(spec/def :spec-swagger/paths
  (spec/map-of string? :spec-swagger/operations))

(spec/def :spec-swagger/operations
  (spec/map-of :spec-swagger/http-method
               :spec-swagger/operation))

(spec/def :spec-swagger/operation
  (spec/and
   (spec/keys :opt-un [:spec-swagger.operation/tags
                       :spec-swagger.operation/summary
                       :spec-swagger.operation/description
                       :spec-swagger.operation/parameters
                       :spec-swagger.operation/responses
                       :spec-swagger.operation/produces
                       :spec-swagger.operation/consumes
                       :spec-swagger.operation/deprecated])
   #(no-extra-keys? [:tags :summary :description
                     :parameters :responses :produces
                     :consumes :deprecated] %)))

(spec/def :spec-swagger.operation/tags (spec/+ string?))
(spec/def :spec-swagger.operation/summary string?)
(spec/def :spec-swagger.operation/description string?)
(spec/def :spec-swagger.operation/parameters
  (spec/keys :opt-un [:spec-swagger.operation.parameter.source/path
                      :spec-swagger.operation.parameter.source/body]))
(spec/def :spec-swagger.operation/responses
  (spec/map-of :spec-swagger/http-response-code
               :spec-swagger.operation/response))
(spec/def :spec-swagger.operation/produces integer?)
(spec/def :spec-swagger.operation/consumes integer?)
(spec/def :spec-swagger.operation/deprecated integer?)

(spec/def :spec-swagger/http-method #{:get :put :post
                                      :delete :options :head})
(spec/def :spec-swagger/http-response-code integer?)

(spec/def :spec-swagger.operation.parameter/source
  #{:spec-swagger.operation.parameter.source/path
    :spec-swagger.operation.parameter.source/body})

(spec/def :spec-swagger.operation/parameter
  (spec/or :normal-parameter :spec-swagger.operation/normal-parameter
           :body-parameter :spec-swagger.operation/body-parameter))

(spec/def :spec-swagger.operation/normal-parameter
  (spec/and :spec-swagger.operation/base-parameter
            (spec/keys
             :req-un [:spec-swagger.operation.parameter/type])))

(spec/def :spec-swagger.operation/body-parameter
  (spec/and :spec-swagger.operation/base-parameter
            (spec/keys
             :req-un [:spec-swagger.definition/schema])))

(spec/def :spec-swagger.operation/base-parameter
  (spec/keys :req-un [:spec-swagger.operation.parameter/name
                      :spec-swagger.operation.parameter/description
                      :spec-swagger.operation.parameter/required]))

(spec/def :spec-swagger.operation.parameter/name string?)
(spec/def :spec-swagger.operation.parameter/description string?)
(spec/def :spec-swagger.operation.parameter/required boolean?)

(spec/def :spec-swagger.operation.parameter/type
  #{"string"})

(spec/def :spec-swagger.operation.parameter.source/path
  (spec/+ :spec-swagger.operation/normal-parameter))

(spec/def :spec-swagger.operation.parameter.source/body
  (spec/tuple :spec-swagger.operation/body-parameter))


(spec/def :spec-swagger.definition/schema
  (spec/keys :req-un [:spec-swagger.definition/schema-name
                      :spec-swagger.definition/spec]))

(spec/def :spec-swagger.definition/schema-name string?)
(spec/def :spec-swagger.definition/spec keyword?)

;
;(defmulti parameter-source :spec-swagger.operation.parameter/source)
;(defmethod parameter-source :spec-swagger.operation.parameter.source/body [_]
;  (spec/and :spec-swagger.operation.parameter/base
;            (spec/keys :req-un [:spec-swagger.operation.parameter/schema])))
;(defmethod parameter-source :spec-swagger.operation.parameter.source/path [_]
;  (spec/+
;   (spec/and :spec-swagger.operation.parameter/base
;             (spec/keys :req-un [:spec-swagger.operation.parameter/type]))))
;
;(spec/def :spec-swagger.operation/parameter-group
;  (spec/multi-spec parameter-source
;                   :spec-swagger.operation.parameter/source))

(spec/def :spec-swagger.operation/response
  (spec/keys :req-un [:spec-swagger.operation.response/description]
             :opt-un [:spec-swagger.definition/schema]))

;(s/defschema Parameters
;             {(opt :body) s/Any
;              (opt :query) s/Any
;              (opt :path) s/Any
;              (opt :header) s/Any
;              (opt :formData) s/Any})

(spec/def :spec-swagger.json/swagger string?)
(spec/def :spec-swagger.json/mime-types (spec/+ string?))
(spec/def :spec-swagger.json/produces :spec-swagger.json/mime-types)
(spec/def :spec-swagger.json/consumes :spec-swagger.json/mime-types)

(spec/def :spec-swagger.json/operations
  (spec/map-of :spec-swagger/http-method
               :spec-swagger.json/operation))

(spec/def :spec-swagger.json/operation
  (spec/keys :req-un [:spec-swagger.json.operation/responses]
             :opt-un [:spec-swagger.operation/summary
                      :spec-swagger.operation/description
                      :spec-swagger.operation/tags
                      :spec-swagger.json.operation/parameters]))

(spec/def :spec-swagger.json.operation/parameter
  (spec/or :normal-parameter :spec-swagger.json.operation/normal-parameter
           :body-parameter :spec-swagger.json.operation/body-parameter))

(spec/def :spec-swagger.json.operation/base-parameter
  (spec/keys :req-un [:spec-swagger.json.operation.parameter/in
                      :spec-swagger.operation.parameter/name
                      :spec-swagger.operation.parameter/description
                      :spec-swagger.operation.parameter/required]))

(spec/def :spec-swagger.json.operation/normal-parameter
  (spec/and
   :spec-swagger.json.operation/base-parameter
   (spec/keys :req-un [:spec-swagger.operation.parameter/type])))

(spec/def :spec-swagger.json.operation/body-parameter
  (spec/and
   :spec-swagger.json.operation/base-parameter
   (spec/keys :req-un [:spec-swagger.json.definition/schema])))

(spec/def :spec-swagger.json.operation.parameter/in
  #{"path" "body"})

(spec/def :spec-swagger.json.operation/responses
  (spec/map-of (spec/or :http-response-code :spec-swagger/http-response-code
                        :default #{:default})
               :spec-swagger.json.operation/response))

(spec/def :spec-swagger.json.operation/response
  (spec/keys :req-un [:spec-swagger.operation.response/description]
             :opt-un [:spec-swagger.json.definition/schema]))

(spec/def :spec-swagger.json.definition/schema
  (spec/keys :req-un [:spec-swagger.json.definition/$ref]))

(spec/def :spec-swagger.json.definition/$ref
  (spec/and string? #(re-matches #"^#/definitions/[\w]+$" %)))


(spec/def :spec-swagger/swagger-json
  (spec/and
   (spec/keys :req-un [:spec-swagger.json/swagger
                       :spec-swagger.json/produces
                       :spec-swagger.json/consumes
                       :spec-swagger.json/info
                       :spec-swagger.json/tags
                       :spec-swagger.json/paths])
   #(no-extra-keys? [:swagger :produces :consumes
                     :info :tags :paths] %)))


(def swagger-defaults {:swagger "2.0"
                       :produces ["application/json"]
                       :consumes ["application/json"]})

(def operation-defaults {:responses {:default {:description ""}}})

;:parameters [{:description ""
;              :in "path"
;              :name "id"
;              :required true
;              :type "string"}
;             {:description ""
;              :in "body"
;              :name "User"
;              :required true
;              :schema {:$ref "#/definitions/User"}}]

;:parameters {:spec-swagger.operation.parameter.source/body {:description "BODY PARAM DESC"
;                                                            :name "BODY PARAM"
;                                                            :schema "BODY SCHEMA"
;                                                            :spec-swagger.operation.parameter/source :spec-swagger.operation.parameter.source/body}
;             :spec-swagger.operation.parameter.source/path [{:description "PATH PARAM DESC"
;                                                             :name "PATH PARAM"
;                                                             :spec-swagger.operation.parameter/source :spec-swagger.operation.parameter.source/path
;                                                             :type "string"}


(defn transform-schema
  [s]
  (if s
    {:$ref (str "#/definitions/" (:schema-name s))}))

(spec/fdef transform-schema
           :args (spec/cat :s (spec/nilable :spec-swagger.definition/schema))
           :ret (spec/nilable :spec-swagger.json.definition/schema))


(defn transform-parameters-for-source
  [[source parameters]]
  (println "SOURCE:" source)
  (println "PARAMS:" parameters)
  (for [p parameters]
    (-> #spy/d {:in (name source)
         :name #spy/d (:name #spy/d p)
         :description (:description p)
         :required (:required p)}
        (assoc-if-not-nil :type (:type p))
        (assoc-if-not-nil :schema (transform-schema (:schema p))))))

(spec/fdef transform-parameters-for-source
           :args (spec/cat
                  :source-params
                  (spec/spec
                   (spec/cat :source :spec-swagger.operation.parameter/source
                             :parameters (spec/spec (spec/+ :spec-swagger.operation/parameter)))))
           :ret (spec/+ :spec-swagger.json.operation/parameter))

(defn transform-parameters
  [parameters]
  (vec (mapcat transform-parameters-for-source parameters)))

(defn transform-response
  [r]
  (-> {:description (:description r)}
      (assoc-if-not-nil :schema (transform-schema (:schema r)))))

(defn transform-responses
  [responses]
  (reduce-kv
   (fn [acc code r]
     (assoc acc code (transform-response r)))
   {}
   responses))

(spec/fdef transform-responses
           :args (spec/cat
                  :responses
                  (spec/nilable :spec-swagger.operation/responses))
           :ret (spec/nilable :spec-swagger.json.operation/responses))

(defn transform-operation
  [operation]
  (println "TRANSFORMING OPERATION:" operation)
  (merge
   operation-defaults
   (-> operation
       (assoc-if-not-empty
        :parameters
        (transform-parameters (:parameters operation)))
       (assoc-if-not-empty
        :responses
        (transform-responses (:responses operation))))))

(spec/fdef transform-operation
           :args (spec/cat :operation :spec-swagger/operation)
           :ret :spec-swagger.json/operation)


(defn transform-operations
  [operations]
  (println "TRANSFORMING OPERATIONS:" operations)
  (reduce-kv
   (fn [acc k v]
     (println "ABOUT TO TRANSFORM: " v)
     (assoc acc k (transform-operation v)))
   {}
   operations))

(spec/fdef transform-operations
           :args (spec/cat :operations :spec-swagger/operations)
           :ret :spec-swagger.json/operations)

(defn transform-paths
  [paths]
  (reduce-kv
   (fn [acc route operations]
     (assoc acc route (transform-operations operations)))
   {}
   paths))

(spec/fdef transform-paths
           :args (spec/cat :paths :spec-swagger/paths))

;:definitions {"User" {:additionalProperties false
;                      :properties {:address {:$ref "#/definitions/UserAddress"}
;                                   :id {:type "string"}
;                                   :name {:type "string"}}
;                      :required :address
;                      :type "object"}
;              "UserAddress" {:additionalProperties false
;                             :properties {:city {:enum nil
;                                                 :type "string"}
;                                          :street {:type "string"}}
;                             :required nil
;                             :type "object"}}

;(schema/defschema User {:id schema/Str,
;                        :name schema/Str
;                        :address {:street schema/Str
;                                  :city (schema/enum :tre :hki)}})

(defn find-schemas-for-parameters
  [parameters]
  (->> #spy/d parameters
       (filter #(contains? % :schema))
       (map :schema)
       set))

;; TODO consolidate
(defn find-schemas-for-responses
  [responses]
  (->> responses
       (filter #(contains? % :schema))
       (map :schema)
       set))

(defn find-schemas-for-operations
  [operations]
  (reduce
   (fn [acc operation]
     (set/union
      acc
      (find-schemas-for-parameters (flatten (vals (:parameters operation))))
      (find-schemas-for-responses (vals (:responses operation)))))
   #{}
   operations))

(defn find-schemas
  [paths]
  (reduce
   (fn [acc path]
     (set/union acc (find-schemas-for-operations #spy/d (vals path))))
   #{}
   (vals paths)))

(declare get-keys-from-spec)

(defn get-keys-from-seq-spec-desc
  [spec-desc]
  (println "get keys from seq spec:" spec-desc)
  (condp = (first spec-desc)
        ;; need to deal with key specs that have both req and opt
        'keys (set (nth spec-desc 2))
        'and  (set (mapcat get-keys-from-spec-desc (rest spec-desc)))

        (throw (IllegalStateException.
                (str "Unsupported spec:" (prn-str spec-desc))))))

(defn get-keys-from-spec-desc
  [spec-desc]
  (println "getting keys from spec-desc:" spec-desc)
  (println "spec desc is coll?:" (coll? spec-desc))
  (if (coll? spec-desc)
    (get-keys-from-seq-spec-desc spec-desc)
    (cond
      (keyword? spec-desc)
      (get-keys-from-spec spec-desc)

      :else
      (throw (IllegalStateException.
              (str "Unsupported spec: " (prn-str spec-desc)))))))

(defn get-keys-from-spec
  [spec]
  (println "getting keys from spec:" spec)
  (let [spec-desc (spec/describe spec)]
    (println "spec desc:" spec-desc)
    (if (= :clojure.spec/unknown spec-desc)
      (throw (IllegalStateException.
              (str "Unable to find spec definition for: '" spec "'")))
      (get-keys-from-spec-desc spec-desc))))

(defn get-type-for-seq-spec-desc
  [spec spec-desc]
  ;; TODO: check that spec-desc is a map
  {:type {:$ref (str "#/definitions/" (name spec))}
   :ref spec})

(defn get-type-for-spec
  [spec]
  (let [spec-desc (spec/describe spec)]
    (println "Creating type for spec desc:" spec-desc)
    (if (coll? spec-desc)
      (get-type-for-seq-spec-desc spec spec-desc)
      {:type {:type
              (condp = spec-desc
                'string? "string"
                'integer? "integer"

                (throw (IllegalStateException. (str "Unable to create type for spec " (pr-str spec-desc)))))}
       :ref nil})))

(defn get-props-and-refs
  [spec-keys]
  (reduce
   (fn [acc k]
     (println "adding spec for k:" k)
     (let [{:keys [type ref]} (get-type-for-spec k)
           acc (assoc-in acc
                         [:props (keyword (name k))]
                         type)]
       (if ref
         (do
           (println "ADDING REF DEF:" ref)
           (update-in acc [:refs] conj ref))
         acc)))
   {:props {}
    :refs []}
   spec-keys))

(defn build-definitions
  [schema]
  (let [spec-keys (get-keys-from-spec (:spec schema))
        schema-name (:schema-name schema)]
    (println "Spec keys:" spec-keys)
    (let [{:keys [props refs]} (get-props-and-refs spec-keys)
          result [[schema-name
                   {:additionalProperties false
                    :type "object"
                    :properties props}]]]
      (if (empty? refs)
        result
        (do
          (println "SHOULD BE DOING SOMETHING WITH REFS:" refs)
          (vec (concat result (map build-definitions refs))))))))

(defn extract-definitions
  [paths]
  (let [schemas (find-schemas paths)]
    (reduce
     (fn [acc schema]
       (let [defs (build-definitions schema)]
         (println "DEFS:" defs)
         (reduce
          (fn [acc [n s]]
            (assoc acc n s))
          acc
          defs)))
     {}
     schemas)))

(defn spec-swagger-json
  [swagger]
  #_(transform-paths (:paths swagger))
  (merge
   swagger-defaults
   (-> swagger
       (assoc :paths (transform-paths (:paths swagger)))))
  #_(let [[paths definitions] (transform-paths (:paths swagger))]
    (merge
     swagger-defaults
     (-> swagger
         (assoc :paths paths)
         (assoc :definitions definitions)))))

(spec/fdef spec-swagger-json
           :args (spec/cat :swagger :spec-swagger/swagger)
           :ret :spec-swagger/swagger-json)

#_(spec/instrument-ns 'spec-swagger-json)

(defn swagger-json2
  []
  {:body (spec-swagger-json
          {:info {:version "1.0.0"
                  :title "Sausages"
                  :description "Sausage description"
                  :termsOfService "http://helloreverb.com/terms/"
                  :contact {:name "My API Team"
                            :email "foo@example.com"
                            :url "http://www.metosin.fi"}
                  :license {:name "Eclipse Public License"
                            :url "http://www.eclipse.org/legal/epl-v10.html"}}
           :tags [{:name "user"
                   :description "User stuff"}]
           :paths {"/api/ping" {:get {}}
                   "/user/:id" {:post {:summary "User Api"
                                       :description "User Api description"
                                       :tags ["user"]
                                       :parameters {:spec-swagger.operation.parameter.source/path
                                                    [{:spec-swagger.operation.parameter/source
                                                      :spec-swagger.operation.parameter.source/path
                                                      :name "PATH PARAM"
                                                      :description "PATH PARAM DESC"
                                                      :required true
                                                      :type "string"}]
                                                    :spec-swagger.operation.parameter.source/body
                                                    [{:spec-swagger.operation.parameter/source
                                                      :spec-swagger.operation.parameter.source/body
                                                      :name "BODY PARAM"
                                                      :description "BODY PARAM DESC"
                                                      :required true
                                                      :schema {:schema-name "User"
                                                               :spec ::test-user-schema}}]},
                                       :responses {200 {:schema {:schema-name "User"
                                                                 :spec ::test-user-schema}
                                                        :description "Found it!"}
                                                   404 {:description "Ohnoes."}}}}}})})

#_(swagger-json2)

(defn swagger-json2-handler
  [req]
  (swagger-json2))


(spec/instrument-ns 'cprice404.swagger-ui-service)



(tk/defservice swagger-ui-service
  [[:WebroutingService add-ring-handler]]
  (init [this context]
        (add-ring-handler this (swagger-ui-handler) {:route-id :swagger})
        (add-ring-handler this (wrap-json-response swagger-json2-handler) {:route-id :swagger-json})
        context))
(ns cprice404.swagger-ui-service
  (:require [puppetlabs.trapperkeeper.core :as tk]
            [ring.swagger.ui :as ring-swagger-ui]
            [ring.swagger.swagger2 :as rs]
            [schema.core :as schema]
            [ring.middleware.json :refer [wrap-json-response]]
            [clojure.spec :as spec]
            [clojure.set :as set]))

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

(defn no-extra-keys?
  [expected-keys m]
  (empty? (set/difference (set (keys m)) (set expected-keys))))

(spec/def :spec-swagger/swagger
  (spec/keys :opt-un [:spec-swagger/info :spec-swagger/paths]))

(spec/def :spec-swagger/info
  (spec/keys :req-un [:spec-swagger.info/title
                      :spec-swagger.info/version]
             :opt-un [:spec-swagger.info/description]))

(spec/def :spec-swagger/swagger-json
  (spec/and
   (spec/keys :opt-un [:spec-swagger.json/info
                       :spec-swagger.json/tags
                       :spec-swagger.json/paths])
   #(no-extra-keys? [:info :tags :paths] %)))

(defn spec-swagger-json
  [swagger]
  swagger)

(spec/fdef spec-swagger-json
           :args (spec/cat :swagger :spec-swagger/swagger)
           :ret :spec-swagger/swagger-json)

(spec/instrument 'spec-swagger-json)

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
                                       :parameters {:path {:id "STRING SPEC HERE"}
                                                    :body "USER SPEC HERE"}
                                       :responses {200 {:schema "USER SPEC HERE"
                                                        :description "Found it!"}
                                                   404 {:description "Ohnoes."}}}}}})})

#_(swagger-json2)

(defn swagger-json2-handler
  [req]
  (swagger-json2))


;(spec/instrument-ns 'cprice404.swagger-ui-service)



(tk/defservice swagger-ui-service
  [[:WebroutingService add-ring-handler]]
  (init [this context]
        (add-ring-handler this (swagger-ui-handler) {:route-id :swagger})
        (add-ring-handler this (wrap-json-response swagger-json2-handler) {:route-id :swagger-json})
        context))
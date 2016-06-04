(ns cprice404.swagger-ui-service
  (:require [puppetlabs.trapperkeeper.core :as tk]
            [ring.swagger.ui :as ring-swagger-ui]
            [ring.swagger.swagger2 :as rs]
            [schema.core :as s]
            [ring.middleware.json :refer [wrap-json-response]]))

(defn swagger-ui-handler
  []
  (ring-swagger-ui/swagger-ui "/docs"))

(s/defschema User {:id s/Str,
                   :name s/Str
                   :address {:street s/Str
                             :city (s/enum :tre :hki)}})

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
                                       :parameters {:path {:id s/Str}
                                                    :body User}
                                       :responses {200 {:schema User
                                                        :description "Found it!"}
                                                   404 {:description "Ohnoes."}}}}}})})

(defn swagger-json1-handler
  [req]
  (swagger-json1))

(tk/defservice swagger-ui-service
  [[:WebroutingService add-ring-handler]]
  (init [this context]
        (add-ring-handler this (swagger-ui-handler) {:route-id :swagger})
        (add-ring-handler this (wrap-json-response swagger-json1-handler) {:route-id :json1})
        context))
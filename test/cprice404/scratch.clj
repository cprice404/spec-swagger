(ns cprice404.scratch
  (:require [schema.core :as s]
            [ring.swagger.swagger2 :as rs]))

(s/defschema User {:id s/Str,
                   :name s/Str
                   :address {:street s/Str
                             :city (s/enum :tre :hki)}})

(clojure.pprint/pprint
 (s/with-fn-validation
  (rs/swagger-json
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
                                            404 {:description "Ohnoes."}}}}}})))
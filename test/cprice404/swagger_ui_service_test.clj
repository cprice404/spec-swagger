(ns cprice404.swagger-ui-service-test
  (:require [clojure.test :refer :all]
            [cprice404.swagger-ui-service :as svc]
            [clojure.spec :as spec]
            [ring.swagger.validator :as swagger-validator]))

;(def.user
;  {:schema-name "User"
;   :spec :swagger-ui-service-test/user})

(spec/def :swagger-ui-service-test/user
  (spec/keys :req-un [:swagger-ui-service-test.user/id
                      :swagger-ui-service-test.user/name
                      :swagger-ui-service-test.user/address]))

(spec/def :swagger-ui-service-test.user/id string?)
(spec/def :swagger-ui-service-test.user/name string?)

(spec/def :swagger-ui-service-test.user/address
  (spec/keys :req-un [:swagger-ui-service-test.user.address/street
                      :swagger-ui-service-test.user.address/city]))

(spec/def :swagger-ui-service-test.user.address/street string?)
(spec/def :swagger-ui-service-test.user.address/city
  #{"Portland" "Austin" "Belfast"})

;(def test-user-with-age-schema
;  {:schema-name "UserWithAge"
;   :spec :swagger-ui-service-test/user-with-age})

  (spec/def :swagger-ui-service-test/user-with-age
  (spec/and
   :swagger-ui-service-test/user
   (spec/keys :req-un [:swagger-ui-service-test.user/age])))

(spec/def :swagger-ui-service-test.user/age integer?)

(def sample-operation
  {:summary "User Api"
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
                  :schema :swagger-ui-service-test/user-with-age}]},
   :responses {200 {:schema :swagger-ui-service-test/user
                    :description "Found it!"}
               404 {:description "Ohnoes."}}})

(def transformed-sample-operation
  {:summary "User Api",
   :description "User Api description",
   :tags ["user"],
   :parameters [{:in "path",
                 :name "PATH PARAM",
                 :description "PATH PARAM DESC",
                 :required true,
                 :type "string"}
                {:in "body",
                 :name "BODY PARAM",
                 :description "BODY PARAM DESC",
                 :required true,
                 :schema {:$ref "#/definitions/UserWithAge"}}],
   :responses {200 {:schema {:$ref "#/definitions/User"},
                    :description "Found it!"},
               404 {:description "Ohnoes."}}})

(deftest operation-spec-test
  (testing "basic operation spec"
    (is (spec/valid? :spec-swagger/operation sample-operation)
        (spec/explain :spec-swagger/operation sample-operation))))

(deftest transform-operations-test
  (testing "basic get"
    (let [input {:get {}}
          transformed (svc/transform-operations input)]
      (is (= {:get {:responses {:default {:description ""}}}}
             transformed))))
  (testing "post with params"
    (let [input {:post sample-operation}
          transformed (svc/transform-operations input)]
      (is (= {:post transformed-sample-operation}
           transformed)))))

(deftest find-schemas-test
  (testing "can find schemas in parameters and responses"
    (is (= #{:swagger-ui-service-test/user
             :swagger-ui-service-test/user-with-age}
           (svc/find-schemas {"/api/ping" {:get {}}
                              "/user/{id}" {:post sample-operation}})))))

(deftest get-keys-from-spec-test
  (testing "simple spec with keys"
    (is (= #{:swagger-ui-service-test.user/id
             :swagger-ui-service-test.user/name
             :swagger-ui-service-test.user/address}
           (svc/get-keys-from-spec
            :swagger-ui-service-test/user))))
  (testing "spec which 'and's in keys"
    (is (= #{:swagger-ui-service-test.user/id
             :swagger-ui-service-test.user/name
             :swagger-ui-service-test.user/address
             :swagger-ui-service-test.user/age}
           (svc/get-keys-from-spec
            :swagger-ui-service-test/user-with-age)))))

(deftest extract-definitions-test
  (testing "able to extract definitions from paths"
    (is (= {"user" {:type "object"
                    :properties {:id {:type "string"}
                                 :name {:type "string"}
                                 :address {:$ref "#/definitions/address"}}
                    :additionalProperties false}
            "user-with-age" {:type "object"
                           :properties {:id {:type "string"}
                                        :name {:type "string"}
                                        :age {:type "integer"}
                                        :address {:$ref "#/definitions/address"}}
                           :additionalProperties false}
            "address" {:type "object"
                           :properties {:street {:type "string"}
                                        :city {:type "string"
                                               :enum #{"Belfast"
                                                       "Austin"
                                                       "Portland"}}}
                           :additionalProperties false}}
           (-> (svc/extract-definitions
                {"/api/ping" {:get {}}
                 "/user/{id}" {:post sample-operation}})
               (update-in ["address" :properties :city :enum] set))))))

(deftest spec-swagger-json-test
  (testing "can generate valid swagger json"
    (let [result
          (svc/spec-swagger-json
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
                    "/user/{id}" {:post sample-operation}}})]
      (is (= {:swagger "2.0",
              :info {:title "Sausages",
                     :version "1.0.0",
                     :description "Sausage description",
                     :termsOfService "http://helloreverb.com/terms/",
                     :contact {:name "My API Team",
                               :email "foo@example.com",
                               :url "http://www.metosin.fi"},
                     :license {:name "Eclipse Public License",
                               :url "http://www.eclipse.org/legal/epl-v10.html"}},
              :produces ["application/json"],
              :consumes ["application/json"],
              :tags [{:name "user", :description "User stuff"}],
              :paths {"/api/ping" {:get {:responses {:default {:description ""}}}},
                      "/user/{id}" {:post transformed-sample-operation}},
              :definitions {"User" {:type "object",
                                    :properties {:id {:type "string"},
                                                 :name {:type "string"},
                                                 :address {:$ref "#/definitions/UserAddress"}},
                                    :additionalProperties false,
                                    :required (:id :name :address)},
                            "UserAddress" {:type "object",
                                           :properties {:street {:type "string"},
                                                        :city {:type "string",
                                                               :enum (:tre :hki)}},
                                           :additionalProperties false,
                                           :required (:street :city)}}}
             result))
      (is (nil? (swagger-validator/validate result))))))


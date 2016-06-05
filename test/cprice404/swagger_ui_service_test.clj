(ns cprice404.swagger-ui-service-test
  (:require [clojure.test :refer :all]
            [cprice404.swagger-ui-service :as svc]
            [clojure.spec :as spec]
            [ring.swagger.validator :as swagger-validator]))

(def test-user-schema
  {:schema-name "User"
   :spec :swagger-ui-service-test/test-user-schema})

(spec/def :swagger-ui-service-test/test-user-schema
  (spec/keys :req-un [:swagger-ui-test.test-user-schema/id
                      :swagger-ui-test.test-user-schema/name
                      :swagger-ui-test.test-user-schema/address]))

(spec/def :swagger-ui-test.test-user-schema/id string?)
(spec/def :swagger-ui-test.test-user-schema/name string?)

(spec/def :swagger-ui-test.test-user-schema/address
  (spec/keys :req-un [:swagger-ui-test.test-user-schema.address/street
                      :swagger-ui-test.test-user-schema.address/city]))

(def test-user-with-age-schema
  {:schema-name "UserWithAge"
   :spec :swagger-ui-service-test/test-user-with-age-schema})

(spec/def :swagger-ui-service-test/test-user-with-age-schema
  (spec/and
   :swagger-ui-service-test/test-user-schema
   (spec/keys :req-un [:swagger-ui-test.test-user-schema/age])))

(spec/def :swagger-ui-test.test-user-schema/age integer?)

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
                  :schema test-user-with-age-schema}]},
   :responses {200 {:schema test-user-schema
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
    (is (= #{test-user-schema test-user-with-age-schema}
           (svc/find-schemas {"/api/ping" {:get {}}
                              "/user/{id}" {:post sample-operation}})))))

(deftest get-keys-from-spec-test
  (testing "simple spec with keys"
    (is (= #{:swagger-ui-test.test-user-schema/id
             :swagger-ui-test.test-user-schema/name
             :swagger-ui-test.test-user-schema/address}
           (svc/get-keys-from-spec
            :swagger-ui-service-test/test-user-schema))))
  (testing "spec which 'and's in keys"
    (is (= #{:swagger-ui-test.test-user-schema/id
             :swagger-ui-test.test-user-schema/name
             :swagger-ui-test.test-user-schema/address
             :swagger-ui-test.test-user-schema/age}
           (svc/get-keys-from-spec
            :swagger-ui-service-test/test-user-with-age-schema)))))

(deftest extract-definitions-test
  (testing "able to extract definitions from paths"
    (is (= {"User" {:type "object"
                    :properties {:id {:type "string"}
                                 :name {:type "string"}
                                 :address {:$ref "#/definitions/address"}}
                    :additionalProperties false}
            "UserWithAge" {:type "object"
                           :properties {:id {:type "string"}
                                        :name {:type "string"}
                                        :age {:type "integer"}
                                        :address {:$ref "#/definitions/address"}}
                           :additionalProperties false}
            "UserAddress" {:type "object"
                           :properties {:street {:type "string"}
                                        :city {:type "string"
                                               :enum [:tre :hki]}}
                           :additionalProperties false
                           :required [:street :city]}}
           (svc/extract-definitions
            {"/api/ping" {:get {}}
             "/user/{id}" {:post sample-operation}})))))

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


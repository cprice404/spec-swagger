(ns cprice404.swagger-ui-service-test
  (:require [clojure.test :refer :all]
            [cprice404.swagger-ui-service :as svc]
            [clojure.spec :as spec]))

(def test-user-schema
  {:schema-name "User"
   :spec ::test-user-schema})

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
                  :schema {:schema-name "User"
                           :spec ::test-user-schema}}]},
   :responses {200 {:schema {:schema-name "User"
                             :spec ::test-user-schema}
                    :description "Found it!"}
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
      (is (= {:post {:summary "User Api",
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
                                   :schema {:$ref "#/definitions/User"}}],
                     :responses {200 {:schema {:$ref "#/definitions/User"},
                                      :description "Found it!"},
                                 404 {:description "Ohnoes."}}}}
           transformed)))))


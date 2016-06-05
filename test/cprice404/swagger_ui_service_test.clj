(ns cprice404.swagger-ui-service-test
  (:require [clojure.test :refer :all]
            [cprice404.swagger-ui-service :as svc]))

(deftest transform-operations-test
  (testing "basic get"
    (let [input {:get {}}
          transformed (svc/transform-operations input)]
      (is (= {:get {:responses {:default {:description ""}}}}
             transformed))))
  (testing "post with params"
    (let [input {:post {:summary "User Api"
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
                                       :schema "BODY SCHEMA"}]}
                        :responses {200 {:schema "USER SPEC HERE"
                                         :description "Found it!"}
                                    404 {:description "Ohnoes."}}}}
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


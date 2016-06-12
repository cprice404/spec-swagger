(ns cprice404.swagger-ui-service-test
  (:require [clojure.test :refer :all]
            [cprice404.swagger-ui-service :as svc]
            [clojure.spec :as spec]
            [ring.swagger.validator :as swagger-validator]
            [ring.mock.request :as mock]
            [ring.middleware.params :as params]
            [puppetlabs.comidi.spec :as comidi-spec]
            [compojure.response :as compojure-response]
            [compojure.core :as compojure]
            [puppetlabs.kitchensink.core :as ks]))

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
                 :schema {:$ref "#/definitions/user-with-age"}}],
   :responses {200 {:schema {:$ref "#/definitions/user"},
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
      (is (= {:swagger "2.0"
              :info {:title "Sausages"
                     :version "1.0.0"
                     :description "Sausage description"
                     :termsOfService "http://helloreverb.com/terms/"
                     :contact {:name "My API Team"
                               :email "foo@example.com"
                               :url "http://www.metosin.fi"}
                     :license {:name "Eclipse Public License"
                               :url "http://www.eclipse.org/legal/epl-v10.html"}}
              :produces ["application/json"]
              :consumes ["application/json"]
              :tags [{:name "user" :description "User stuff"}]
              :paths {"/api/ping" {:get {:responses {:default {:description ""}}}}
                      "/user/{id}" {:post transformed-sample-operation}}
              :definitions {"user" {:type "object"
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
                                                                   "Portland"
                                                                   "Austin"}}}
                                       :additionalProperties false}}}
             (-> result
                 (update-in
                  [:definitions "address" :properties :city :enum]
                  set))))
      (is (nil? (swagger-validator/validate result))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def scratch-routes
  (comidi-spec/context "/foo"
    (comidi-spec/GET "/plus"
                     ;; TODO: maybe have the macro inject the `spec/spec` part here?
                     {:return (spec/spec integer?)
                      :query-params [:foo-handler/x :foo-handler/y]
                      :summary "x+y with query-parameters"}
                     {:body (str (+ (Integer/parseInt x)
                                    (Integer/parseInt y)))})))

(def scratch-handler
  (params/wrap-params
   (comidi-spec/routes->handler
    scratch-routes)))

(deftest scratch-handler-test
  (testing "plus works"
    (let [req (mock/request :get "/foo/plus?x=4&y=2")]
      (is (= "6"
             (:body (scratch-handler req)))))))

(deftest scratch-handler-specs-test
  (testing "specs are attached to routes as metadata"
    ;; TODO: do this with the zipper?
    (let [plus-route (first (second scratch-routes))
          plus-route-meta (meta plus-route)]
      (is (= #{:return :query-params :summary}
             (set (keys plus-route-meta))))
      (is (= "x+y with query-parameters" (:summary plus-route-meta)))
      (is (= [:foo-handler/x :foo-handler/y]
             (:query-params plus-route-meta)))
      (is (= 'integer? (spec/describe (:return plus-route-meta)))))))

(deftest scratch-handler-register-paths-test
  (testing "Can register paths based on route metadata"
    (is (= {"/foo/plus" {:get
                         {:responses
                          {200 {:description ""
                                :schema (spec/describe (spec/spec integer?))}}}}}
           (-> (svc/register-paths* (atom {})
                                 (comidi-spec/swagger-paths
                                  scratch-routes))
               (update-in ["/foo/plus" :get :responses 200 :schema]
                          spec/describe))))))



(ns cprice404.swagger-ui-service-test
  (:require [clojure.test :refer :all]
            [cprice404.swagger-ui-service :as svc]))

(deftest transform-operations-test
  (testing "basic get"
    (let [input {:get {}}
          transformed (svc/transform-operations input)]
      (is (= {:get {:responses {:default {:description ""}}}}
             transformed)))))

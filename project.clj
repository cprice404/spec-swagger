(defproject cprice404/spec-swagger "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha4"]

                 [metosin/ring-swagger "0.22.8"]
                 [metosin/ring-swagger-ui "2.1.4-0"]
                 [ring/ring-json "0.4.0"]

                 [puppetlabs/trapperkeeper "1.4.1"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "1.5.9"]

                 [puppetlabs/comidi "0.3.1"]

                 [ring/ring-core "1.4.0"]

                 ]

  :main puppetlabs.trapperkeeper.main

  )

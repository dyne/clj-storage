(defproject org.clojars.dyne/clj-storage "0.11.0-SNAPSHOT"
  :description "Minimal storage library based on an abstraction which helps seamlessly switching between DBs"
  :url "https://github.com/PIENews/clj-storage"

  :license {:author "Dyne.org Foundation"
            :email "foundation@dyne.org"
            :year 2017
            :key "gpl-3.0"}

  :dependencies [[org.clojure/clojure "1.10.1"]

                 ;; logs
                 [com.taoensso/timbre "4.10.0"]

                 ;; mongodb
                 [org.clojars.dyne/monger "3.2.0-SNAPSHOT"]
                 [cheshire "5.3.1"]

                 ;; file storage
                 [com.taoensso/nippy "2.14.0"]

                 ;; redis
                 [com.taoensso/carmine "2.19.1"]

                 ;; sql
                 [seancorfield/next.jdbc "1.0.13"]
                 ;; driver for sqlite 
                 [org.xerial/sqlite-jdbc "3.30.1"]]

  :source-paths ["src" "test"]
  :resource-paths ["resources" "test-resources"]
  :aliases {"test" ["midje"]
            "test-basic" ["midje" ":config" "test-resources/fast-tests.config"]}
  :profiles {:dev {:dependencies [[midje "1.9.9"]]
                   :plugins [[lein-midje "3.2"]]}})

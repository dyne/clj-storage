(defproject org.clojars.dyne/clj-storage "0.11.0-SNAPSHOT"
  :description "Minimal storage library"
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

                 ;; file storage
                 [com.taoensso/nippy "2.14.0"]

                 ;; redis
                 [com.taoensso/carmine "2.19.1"]]

  :source-paths ["src" "test"]
  :resource-paths ["resources" "test-resources"]
  :profiles {:dev {:dependencies [[midje "1.9.9"]]
                   :plugins [[lein-midje "3.1.3"]]}})

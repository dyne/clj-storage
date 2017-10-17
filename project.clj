(defproject org.clojars.dyne/clj-storage "0.3.0"
  :description "Minimal storage library"
  :url "https://github.com/PIENews/clj-storage"

  :license {:author "Dyne.org Foundation"
            :email "foundation@dyne.org"
            :year 2017
            :key "gpl-3.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; logs
                 [com.taoensso/timbre "4.10.0"]

                 ;; mongodb
                 [com.novemberain/monger "3.1.0"]]

  :source-paths ["src"]
  :resource-paths ["resources" "test-resources"]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]
                   :plugins [[lein-midje "3.1.3"]]}})

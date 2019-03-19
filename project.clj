(defproject org.clojars.dyne/clj-storage "0.10.0-SNAPSHOT"
  :description "Minimal storage library"
  :url "https://github.com/PIENews/clj-storage"

  :license {:author "Dyne.org Foundation"
            :email "foundation@dyne.org"
            :year 2017
            :key "gpl-3.0"}

  ;; fix for jdk 10
  :managed-dependencies [[org.clojure/core.rrb-vector "0.0.14"]
                       [org.flatland/ordered "1.5.7"]]

  :dependencies [[org.clojure/clojure "1.10.0"]

                 ;; logs
                 [com.taoensso/timbre "4.10.0"]

                 ;; mongodb
                 [org.clojars.dyne/monger "3.2.0-SNAPSHOT"]

                 ;; file storage
                 [com.taoensso/nippy "2.14.0"]
]

  :source-paths ["src" "test"]
  :resource-paths ["resources" "test-resources"]
  :profiles {:dev {:dependencies [[midje "1.9.6"]]
                   :plugins [[lein-midje "3.1.3"]]}})

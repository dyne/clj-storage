(ns clj-storage.test.db.mongo
  (:require [midje.sweet :refer :all]
            [monger
             [core :as m]
             [db :as db]
             [collection :as mcol]]
            [clj-storage.db.mongo :refer [create-mongo-stores]]
            [taoensso.timbre :as log]))
(def test-db-name "test-db")
(defonce db-and-conn (atom nil))
(defn setup-db [uri]
  (->> (m/connect-via-uri uri) 
       (reset! db-and-conn)))

(against-background [(before :contents (setup-db (str "mongodb://localhost:27017/" test-db-name)))
                     (after :contents (m/drop-db (:conn @db-and-conn) test-db-name))]

                    (facts "Test the mongo stores creation"
                           (let [name-param-m {"simple-store" {}
                                               "store-with-ttl" {:expireAfterSeconds 30}}]
                             (create-mongo-stores (:db @db-and-conn)
                                                  name-param-m)
                             (db/get-collection-names (:db @db-and-conn)) => #{"simple-store"
                                                                               "store-with-ttl"}
                             (count (mcol/indexes-on (:db @db-and-conn) "simple-store")) => 1
                             (count (mcol/indexes-on (:db @db-and-conn) "store-with-ttl")) => 2)))

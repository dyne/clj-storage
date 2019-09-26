;; clj-storage - a minimal storage library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns clj-storage.test.db.mongo
  (:require [midje.sweet :refer [against-background before after facts fact => truthy]]
            [monger
             [core :as m]
             [db :as db]
             [collection :as mcol]]
            [clj-storage.db.mongo :refer [create-mongo-stores drop-db]]
            [clj-storage.core :as storage]
            [clj-storage.test.db.mongo.test-db :as test-db]
            [taoensso.timbre :as log]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]

                    (facts "Test the mongo stores creation"
                           (let [name-param-m {"simple-store" {}
                                               "transaction-store" {}
                                               "store-with-ttl" {:expireAfterSeconds 30}
                                               "store-with-index" {:unique-index [:apikey
                                                                                  :client-app]}}
                                 stores (create-mongo-stores (test-db/get-test-db) 
                                                  name-param-m)]
                             ;; insert document so store is created
                             (storage/store-and-create-id! (:simple-store stores) {:title "some document"})
                             (db/get-collection-names (test-db/get-test-db)) => #{"simple-store"
                                                                                  "store-with-ttl",
                                                                                  "store-with-index"}
                             ;; list-index is not part of the collection names since Mongo 3.07 ; related commit https://github.com/mongodb/mongo/commit/fa24b6adab2f71a3c07d8810d04d5e0da4c5ac59
                             (m/command (test-db/get-test-db) {:listIndexes "simple-store"}) => {"cursor" {"id" 0, "ns" "test-db.$cmd.listIndexes.simple-store", "firstBatch" [{"v" 2, "key" {"_id" 1}, "name" "_id_", "ns" "test-db.simple-store"}]}, "ok" 1.0}

                             (m/command (test-db/get-test-db) {:listIndexes "store-with-ttl"}) => {"cursor" {"id" 0, "ns" "test-db.$cmd.listIndexes.store-with-ttl", "firstBatch" [{"v" 2, "key" {"_id" 1}, "name" "_id_", "ns" "test-db.store-with-ttl"} {"v" 2, "key" {"created-at" 1}, "name" "created-at_1", "ns" "test-db.store-with-ttl", "expireAfterSeconds" 30}]}, "ok" 1.0}

                             (-> (test-db/get-test-db)
                                 (m/command  {:listIndexes "store-with-index"})
                                 (get "cursor")
                                 count) => 3
                             
                             (count (mcol/indexes-on (test-db/get-test-db) "simple-store")) => 1
                             (count (mcol/indexes-on (test-db/get-test-db) "store-with-ttl")) => 2

                             (fact "Test mongo updates." 
                                    (storage/store! (:transaction-store stores) :_id {:_id (rand-int 20000)
                                                                                      :currency :mongo
                                                                                      :from-id "an-account"
                                                                                      :to-id "another-account"
                                                                                      :tags []
                                                                                      :amount 1000
                                                                                      :timestamp (new java.util.Date) 
                                                                                      :transaction-id "1"}) => truthy
                                    (-> (storage/query (:transaction-store stores) {:transaction-id "1"})
                                        first
                                        (dissoc :_id :timestamp)) => {:amount 1000, :currency "mongo", :from-id "an-account", :tags [], :to-id "another-account", :transaction-id "1"}
                                    
                                    (let [item (first (storage/query (:transaction-store stores) {:transaction-id "1"}))
                                          updated-item ((fn [doc] (update doc :amount #(+ % 1))) item)]
                                      (:amount updated-item) => 1001)
                                    (:amount (storage/update! (:transaction-store stores) {:transaction-id "1"} (fn [doc] (update doc :amount #(+ % 1))))) => 1001)

                             (fact "Test total count."
                                   (storage/count* (:transaction-store stores) {}) => 1
                                   (storage/store! (:transaction-store stores) :_id {:_id (rand-int 20000)
                                                                                     :currency :mongo
                                                                                     :from-id "yet-an-account"
                                                                                     :to-id "another-account"
                                                                                     :tags []
                                                                                     :amount 1000
                                                                                     :timestamp (new java.util.Date)
                                                                                     :transaction-id "2"})
                                   (storage/count* (:transaction-store stores) {}) => 2)

                             (fact "Test that date time filtering works."
                                   (let [now (new java.util.Date)]
                                     (storage/count* (:transaction-store stores) {}) => 2
                                     (-> (storage/list-per-page (:transaction-store stores) {} 1 100) first :timestamp) => truthy
                                     (storage/count* (:transaction-store stores) {:timestamp {"$gt" now}}) => 0
                                     (storage/count* (:transaction-store stores) {:timestamp {"$lt" now}}) => 2)))))

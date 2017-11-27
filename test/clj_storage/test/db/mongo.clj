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
  (:require [midje.sweet :refer :all]
            [monger
             [core :as m]
             [db :as db]
             [collection :as mcol]]
            [clj-storage.db.mongo :refer [create-mongo-stores]]
            [clj-storage.core :as storage]
            [clj-storage.test.db.test-db :as test-db]
            [taoensso.timbre :as log]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]

                    (facts "Test the mongo stores creation"
                           (let [name-param-m {"simple-store" {}
                                               "transaction-store" {}
                                               "store-with-ttl" {:expireAfterSeconds 30}}
                                 stores (create-mongo-stores (test-db/get-test-db) 
                                                  name-param-m)]
                             ;; insert document so store is created
                             (mcol/insert (test-db/get-test-db) "simple-store" {:title "some document"})
                             (db/get-collection-names (test-db/get-test-db)) => #{"simple-store"
                                                                               "store-with-ttl"}
                             (count (mcol/indexes-on (test-db/get-test-db) "simple-store")) => 1
                             (count (mcol/indexes-on (test-db/get-test-db) "store-with-ttl")) => 2

                             (fact "Test mongo updates" 
                                    (storage/store! (:transaction-store stores) :_id {:_id (rand-int 20000)
                                                                                      :currency :mongo
                                                                                      :from-id "an-account"
                                                                                      :to-id "another-account"
                                                                                      :tags []
                                                                                      :amount 1000
                                                                                      :transaction-id "1"}) => truthy
                                    (-> (mcol/find-one-as-map (test-db/get-test-db) "transaction-store" {:transaction-id "1"}) (dissoc :_id)) => {:amount 1000, :currency "mongo", :from-id "an-account", :tags [], :to-id "another-account", :transaction-id "1"}
                                    (let [item (mcol/find-one-as-map (test-db/get-test-db) "transaction-store" {:transaction-id "1"})
                                          updated-item ((fn [doc] (update doc :amount #(+ % 1))) item)]
                                      (:amount updated-item) => 1001)
                                    (:amount (storage/update! (:transaction-store stores) {:transaction-id "1"} (fn [doc] (update doc :amount #(+ % 1))))) => 1001))))

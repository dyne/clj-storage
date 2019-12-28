;; clj-storage - a minimal storage library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2019- Dyne.org foundation

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

(ns clj-storage.test.db.redis
  (:require [midje.sweet :refer [against-background before after facts fact => truthy]]

            [clj-storage.core :as storage]
            [clj-storage.db.redis :as redis]
            [clj-storage.test.db.redis.test-db :as test-db]
            [clj-storage.spec]

            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :as log]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]

                    (facts "Test the redis protocol implemetation"
                           (let [hardcoded-id "one-id"]

                             (fact "Test mongo stores creation and fetch"
                                   ;; insert document so store is created
                                   (storage/store! (test-db/get-test-store) {:key "foo"
                                                                             :value "bar"}) => "OK"
                                   (storage/query (test-db/get-test-store) {:key "foo"} {}) => "bar"
                                   (redis/count-keys (test-db/get-test-store)) => 1
                                   (redis/get-all-keys (test-db/get-test-store)) => ["foo"])

                             (fact "Add more key-value pairs and do a query that results in multiple"
                                   (doseq [n (range 10)]
                                     (storage/store! (test-db/get-test-store) {:key (java.util.UUID/randomUUID)
                                                                               :value (str "value" n)}) => "OK")
                                   (redis/count-keys (test-db/get-test-store)) => 11

                                   (let [ks (redis/get-all-keys (test-db/get-test-store))]
                                     (count (storage/query (test-db/get-test-store) {:keys (take 5 ks)} {})) => 5))

                             #_(facts "Test expiration" :slow
                                    (fact "Wait for 90 seconds to check that item is deleted after expiration" :slow
                                          
                                          ;; TTL is set to 30 seconds but mongo checks only every ~60 secs
                                          (Thread/sleep (* 90 1000))
                                          (count (storage/query (:store-with-ttl stores) {} {})) => 0)))))

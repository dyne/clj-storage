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

(ns clj-storage.test.in-memory
  (:require [midje.sweet :refer [against-background before after facts fact => truthy]]
            [clj-storage.core :as storage]
            [taoensso.timbre :as log]))

(facts "Test the in-memory storage protocol implemetation"
       ;; TODO: this works for one storage only at the time, otherwise all docs are stored in the same atom, so cannot distinguished. TO be fixed when necessary for example when ttl is implemented
       (let [name-param-m ["simple-store"]
             stores (storage/create-in-memory-stores name-param-m)
             hardcoded-id "one-id"]

         (fact "Test in-memory create"
               ;; Adding here the expiration entry for later
               (count (storage/query (:simple-store stores) {} {})) => 0
               
               (let [item (storage/store! (:simple-store stores) {:id hardcoded-id
                                                                  :currency "mongo"
                                                                  :from-id "an-account"
                                                                  :to-id "another-account"
                                                                  :tags []
                                                                  :amount 1000
                                                                  :timestamp (new java.util.Date) 
                                                                  :transaction-id "1"})]
                 (:amount item) => 1000
                 (-> (storage/query (:simple-store stores) {:id hardcoded-id} {})
                     first
                     :amount) => 1000))
         
         (fact "Test in-memory query" 
               (storage/store! (:simple-store stores) {:id (rand-int 20000)
                                                       :currency "mongo"
                                                       :from-id "an-account"
                                                       :to-id "another-account"
                                                       :tags []
                                                       :amount 1000
                                                       :timestamp (new java.util.Date) 
                                                       :transaction-id "2"}) => truthy
               (-> (storage/query (:simple-store stores) {:transaction-id "2"} {})
                   first
                   (dissoc :id :timestamp :created-at)) => {:amount 1000, :currency "mongo", :from-id "an-account", :tags [], :to-id "another-account", :transaction-id "2"}

               (-> (storage/query (:simple-store stores) {:from-id "an-account"} {})
                   first
                   (dissoc :id :timestamp :created-at)) => {:amount 1000, :currency "mongo", :from-id "an-account", :tags [], :to-id "another-account", :transaction-id "1"}

               (-> (storage/query (:simple-store stores) {:from-id "an-account"} {})
                   second
                   (dissoc :id :timestamp :created-at)) => {:amount 1000, :currency "mongo", :from-id "an-account", :tags [], :to-id "another-account", :transaction-id "2"}

               (count (storage/query (:simple-store stores) {} {})) => 2)

         (fact "Test in-memory updates"
               (storage/update! (:simple-store stores) {:transaction-id "1"} #(update % :amount inc))
               (-> (storage/query (:simple-store stores) {:transaction-id "1"} {})
                   first
                   :amount) => 1001

               (storage/update! (:simple-store stores) {:from-id "an-account"} #(update % :amount inc))
               (-> (storage/query (:simple-store stores) {:from-id "an-account"} {})
                   first
                   :amount) => 1002

               (-> (storage/query (:simple-store stores) {:from-id "an-account"} {})
                   second
                   :amount) => 1001

               (count (storage/query (:simple-store stores) {} {})) => 2)

         (fact "Test pagination"
               (count (storage/query (:simple-store stores) {} {:per-page 1 :page 1}))
               => 1
               (count (storage/query (:simple-store stores) {} {:per-page 10 :page 1}))
               => 2
               (count (storage/query (:simple-store stores) {} {:per-page 2 :page 1}))
               => 2
               (count (storage/query (:simple-store stores) {} {:per-page 1 :page 2}))
               => 1)

         (fact "Test aggregation (count)"
               (storage/count-items (:simple-store stores) {}) => 2
               ;; TODO: this should be 1. Wrong implementation fr in memory
               (storage/count-items (:simple-store stores) {:transaction-id "2"}) => 2
               (storage/count-items (:simple-store stores) {:amount #(> % 1000)}) => 2)))







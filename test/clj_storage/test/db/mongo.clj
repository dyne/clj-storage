(ns clj-storage.test.db.mongo
  (:require [midje.sweet :refer :all]
            [monger
             [core :as m]
             [db :as db]
             [collection :as mcol]]
            [clj-storage.db.mongo :refer [create-mongo-stores]]
            [clj-storage.core :as storage]
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
                                               "transaction-store" {}
                                               "store-with-ttl" {:expireAfterSeconds 30}}
                                 stores (create-mongo-stores (:db @db-and-conn)
                                                  name-param-m)]
                             ;; insert document so store is created
                             (mcol/insert (:db @db-and-conn) "simple-store" {:title "some document"})
                             (db/get-collection-names (:db @db-and-conn)) => #{"simple-store"
                                                                               "store-with-ttl"}
                             (count (mcol/indexes-on (:db @db-and-conn) "simple-store")) => 1
                             (count (mcol/indexes-on (:db @db-and-conn) "store-with-ttl")) => 2

                             (fact "Test mongo updates" 
                                    (storage/store! (:transaction-store stores) :_id {:_id (rand-int 20000)
                                                                                      :currency :mongo
                                                                                      :from-id "an-account"
                                                                                      :to-id "another-account"
                                                                                      :tags []
                                                                                      :amount 1000
                                                                                      :transaction-id "1"}) => truthy
                                    (-> (mcol/find-one-as-map (:db @db-and-conn) "transaction-store" {:transaction-id "1"}) (dissoc :_id)) => {:amount 1000, :currency "mongo", :from-id "an-account", :tags [], :to-id "another-account", :transaction-id "1"}
                                    (let [item (mcol/find-one-as-map (:db @db-and-conn) "transaction-store" {:transaction-id "1"})
                                          updated-item ((fn [doc] (update doc :amount #(+ % 1))) item)]
                                      (:amount updated-item) => 1001)
                                    (:amount (storage/update! (:transaction-store stores) {:transaction-id "1"} (fn [doc] (update doc :amount #(+ % 1))))) => 1001))))

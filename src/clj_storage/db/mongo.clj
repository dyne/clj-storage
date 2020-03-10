;; clj-storage - Minimal storage library based on an abstraction which helps seamlessly switching between DBs

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti  <aspra@dyne.org>

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

(ns clj-storage.db.mongo
  (:require [monger.collection :as mc]
            [monger
             [core :as mongo]
             [collection :as mcol]
             [query :as mq]]
            [monger.operators :refer :all]
            
            [clj-storage.core :as storage :refer [Store]]
            [clj-storage.spec]
            [clojure.spec.alpha :as spec]
            
            [taoensso.timbre :as log]))

(defn get-mongo-db-and-conn [mongo-uri]
  (let [db-and-conn (mongo/connect-via-uri mongo-uri)]
    db-and-conn))

(defn get-mongo-db [mongo-uri]
  (:db (get-mongo-db-and-conn mongo-uri)))

(defn disconnect [db]
  (mongo/disconnect db))

(defn drop-db [db-and-conn]
  (mongo/drop-db (:conn db-and-conn) (-> db-and-conn :db .getName))
  db-and-conn)

(defrecord MongoStore [mongo-db coll]
  Store
  (store! [collection item]
    ;; always add a created-at field, in case we need expiration
    (let [item-with-timestamp (assoc item :created-at (java.util.Date.))]
      (if (and (:id item) (spec/valid? map? item))
        (do (spec/valid? :clj-storage.spec/id (:id item))
            (-> (mc/insert-and-return mongo-db coll (assoc item-with-timestamp :_id (:id item)))
                (dissoc :id)))
        (mc/insert-and-return mongo-db coll item-with-timestamp))))

  (update! [collection query update-fn]
    (mc/update mongo-db coll query update-fn {:multi true}))
  
  (query [collection query pagination]
    (if (empty? pagination)
      (if (spec/valid? :clj-storage.spec/only-id-map query)
        (-> (mc/find-map-by-id mongo-db coll (:id query))
            (dissoc :_id))
        (->> (mc/find-maps mongo-db coll query)
             (map #(dissoc % :_id))))
      (when (spec/valid? :clj-storage.db.mongo/pagination pagination)
        (mq/with-collection mongo-db coll
          (mq/find query)
          (mq/sort {:timestamp -1})
          (mq/paginate :page (:page pagination) :per-page (:per-page pagination))))))
  
  (delete! [collection item]
    (if (spec/valid? :clj-storage.spec/only-id-map item) 
      (mc/remove-by-id mongo-db coll (:id item))
      (mc/remove mongo-db coll item)))

  (aggregate [collection formula params]
    (mc/aggregate mongo-db coll formula))

  (add-index [collection index params]
    (when (spec/valid? ::index-params params)
      (mc/ensure-index mongo-db coll (array-map index 1) {:unique (or false (:unique params))})))

  (expire [collection seconds params]
    (mc/ensure-index mongo-db coll {:created-at 1} {:expireAfterSeconds seconds})))

;; TODO: delete-all?
;; Maybe move this to DB specific file and not the protocol
#_(delete-all! [this]
               (mc/remove mongo-db coll))

(defn count-items [mongo-store query]
  (or (-> (storage/aggregate mongo-store
                             [{"$match" query}
                              {"$group" {:_id nil
                                         :count {"$sum" 1}}}]
                             {})
          first
          :count)
      ;; If no aggregation is made due to match not fitting, return 0
      0))

(defn count-since [mongo-store dt query]
  (or (-> (storage/aggregate mongo-store
                             [{"$match" (merge query {:created-at {"$gt" dt}})}
                              {"$group" {:_id nil
                                         :count {"$sum" 1}}}]
                             {})
          first
          :count)
      ;; If no aggregation is made due to match not fitting, return 0 
      0))

(defn create-mongo-store
  ([mongo-db coll]
   (create-mongo-store mongo-db coll {}))
  ([mongo-db coll {:keys [expireAfterSeconds unique-index]}]
   (let [store (MongoStore. mongo-db coll)]
     (when expireAfterSeconds
       (storage/expire store expireAfterSeconds {}))
     (when unique-index
       (doseq [index unique-index]
         (storage/add-index store index {:unique true})))
     store )))

(defn create-mongo-stores
  [db name-param-m]
  (reduce merge (map
                 #(let [col-name (key %)
                        params-m (val %)] 
                    (hash-map
                     (keyword col-name)
                     (create-mongo-store db col-name params-m)))
                 name-param-m)))

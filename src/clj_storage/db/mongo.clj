;; clj-storage - Minimal storage library based on an abstraction which helps seamlessly switching between DBs

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

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
            [monger.operators :refer [$gt]]
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
  (store! [this item]
    (if (and (:id item) (spec/valid? map? item))
      (do (log/spy (spec/valid? :clj-storage.spec/id (:id item)))
          (log/spy (-> (mc/insert-and-return mongo-db coll (log/spy (assoc item :_id (:id item))))
                       (dissoc :id))))
      (mc/insert-and-return mongo-db coll item)))
  
  (update! [this item update-fn]
    (when-let [item (if-let [id (and (:id item) (spec/valid? :clj-storage.spec/id (:id item)))]
                      (mc/find-map-by-id mongo-db coll id)
                      (mc/find-one-as-map mongo-db coll item))]
      (let [updated-item (update-fn item)]
        (-> (mc/save-and-return mongo-db coll updated-item)
            (dissoc :_id)))))
  
  (query [this query]
    (if (spec/valid? :clj-storage.spec/only-id-map query)
      (-> (mc/find-map-by-id mongo-db coll (:id query))
          (dissoc :_id))
      (->> (mc/find-maps mongo-db coll query)
           (map #(dissoc % :_id)))))

 #_(list-per-page [this query page per-page]
    (vec (map
          ;; TODO: can this be done by the monger query lib?
          #(dissoc % :_id)
          (mq/with-collection mongo-db coll
            (mq/find query)
            (mq/sort {:timestamp -1})
            (mq/paginate :page page :per-page per-page)))))
  
  (delete! [this item]
    (if (spec/valid? (:clj-storage.spec/only-id-map item)) 
      (mc/remove-by-id mongo-db coll (:id item))
      (mc/remove mongo-db coll item)))

  ;; Maybe move this to DB specific file and not the protocol
  #_(delete-all! [this]
    (mc/remove mongo-db coll))

  (aggregate [this formula params]
    (mc/aggregate mongo-db coll formula))

  #_(count-since [this from-date-time formula]
    (let [dt-condition {:created-at {$gt from-date-time}}]
      (mc/count mongo-db coll (merge formula
                                     dt-condition))))

  #_(count* [this formula]
    (mc/count mongo-db coll formula)))

#_(defn count-items-in-col [moongo-store query]
  ())

(defn create-mongo-store
  ([mongo-db coll]
   (create-mongo-store mongo-db coll {}))
  ([mongo-db coll {:keys [expireAfterSeconds unique-index]}]
   (let [store (log/spy (MongoStore. mongo-db coll))]
     (when expireAfterSeconds 
       (mc/ensure-index mongo-db coll {:created-at 1}
                        {:expireAfterSeconds expireAfterSeconds}))
     (when unique-index
       (doseq [index unique-index] 
        (mc/ensure-index mongo-db coll (array-map index 1) {:unique true})))
     store)))

(defn create-mongo-stores
  [db name-param-m]
  (log/spy (reduce merge (map
                          #(let [col-name (key %)
                                 params-m (val %)] 
                             (hash-map
                              (keyword col-name)
                              (create-mongo-store db col-name params-m)))
                          name-param-m))))

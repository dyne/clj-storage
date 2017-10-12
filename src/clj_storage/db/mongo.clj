;; clj-storage - a minimal storage library

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
             [db :as mdb]
             [core :as mongo]
             [collection :as mcol]]
            [clj-storage.core :as storage :refer [Store]]))

(defn get-mongo-db-and-conn [mongo-uri]
  (let [db-and-conn (mongo/connect-via-uri mongo-uri)]
    db-and-conn))

(defn get-mongo-db [mongo-uri]
  (:db (get-mongo-db-and-conn mongo-uri)))

(defn disconnect [db]
  (if (not (empty? db)) (mongo/disconnect db)))

(defrecord MongoStore [mongo-db coll]
  Store
  (store! [this k item]
    (-> (mc/insert-and-return mongo-db coll (assoc item :_id (k item)))
        (dissoc :_id)))

  (update! [this k update-fn]
    (when-let [item (mc/find-map-by-id mongo-db coll k)]
      (let [updated-item (update-fn item)]
        (-> (mc/save-and-return mongo-db coll updated-item)
            (dissoc :_id)))))

  (fetch [this k]
    (when k
      (-> (mc/find-map-by-id mongo-db coll k)
          (dissoc :_id))))

  (query [this query]
    (->> (mc/find-maps mongo-db coll query)
         (map #(dissoc % :_id))))

  (delete! [this k]
    (when k
      (mc/remove-by-id mongo-db coll k)))

  (delete-all! [this]
    (mc/remove mongo-db coll))

  (aggregate [this formula]
    (mc/aggregate mongo-db coll formula)))

(defn create-mongo-store
  ([mongo-db coll]
   (create-mongo-store mongo-db coll {}))
  ([mongo-db coll {:keys [expireAfterSeconds]}]
   (let [store (MongoStore. mongo-db coll)]
     (when expireAfterSeconds 
       (mc/ensure-index mongo-db coll {:created-at 1}
                        {:expireAfterSeconds expireAfterSeconds}))
     store)))

(defn create-mongo-stores
  [db name-param-m]
  (reduce merge (map
                 #(let [col-name (key %)
                        params-m (val %)] 
                    (hash-map
                     (keyword col-name)
                     (create-mongo-store db col-name params-m)))
                 name-param-m)))

(defn empty-db-stores! [stores-m]
  (doseq [col (vals stores-m)]
    (storage/delete-all! col)))

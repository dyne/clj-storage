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

(ns clj-storage.core)

(defprotocol Store
  (store! [s item]
    "Store an item to storage s")
  (update! [s item update-fn]
    "Update the item found by running the update-fn on it and storing it")
  (query [s query]
    "Find one or more items given a query map (does a fetch when query map is only id)")
  (delete! [s item]
    "Delete item from a storage s")
  (aggregate [s formula params]
    "Process data aggregate and return computed results")
  (add-index [s index unique]
    "Add an index to a storage s. Unique can be true or false")
  (expire [s seconds]
    "Expire items of this storage after seconds")) 

#_(defrecord MemoryStore [data]
  Store
  (store! [this item]
    ;; TODO: is k a good solution?
    (do (swap! data assoc ((:k item) item) item)
        item))

  (update! [this item update-fn]
    (when-let [item (@data (:k item))]
      (let [updated-item (update-fn item)]
        (swap! data assoc (:k item) updated-item)
        updated-item)))

  #_(fetch [this k] (@data k))

  ;; TODO: add fetch
  (query [this query]
    (filter #(= query (select-keys % (keys query))) (vals @data)))

  (delete! [this item]
    (swap! data dissoc (:k item)))

  ;; TODO: maybe add as wrapper function?
  #_(delete-all! [this]
    (reset! data {}))

  ;; TODO: add aggregate?
  #_(count-since [this date-time formula]
    ;; TODO: date time add
    (count (filter #(= formula (select-keys % (keys formula))) (vals @data)))))

#_(defn create-memory-store
  "Create a memory store"
  ([] (create-memory-store {}))
  ([data]
   ;; TODO: implement ttl and aggregation
   (MemoryStore. (atom data))))

#_(defn create-in-memory-stores [store-names]
  (zipmap
   (map #(keyword %) store-names)
   (repeat (count store-names) (create-memory-store))))

;; TODO
#_(defn empty-db-stores! [stores-m]
  (doseq [col (vals stores-m)]
    (delete-all! col)))

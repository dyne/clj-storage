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


(ns clj-storage.db.sqlite
  (:require [clj-storage.core :as storage :refer [Store]]
            [clj-storage.spec]
            [next.jdbc :as sql]
            [clj-storage.spec]
            [clojure.spec.alpha :as spec]
            ; [taoensso.timbre :as log]
            ))


(defrecord SqliteStore [ds]
  Store
  (store! [this item]
          ;; always add a created-at field, in case we need expiration
    (let [item-with-timestamp (assoc item :created-at (java.util.Date.))]
      (sql/insert! ds this item-with-timestamp)))
  (update! [this query update-fn]
    (sql/update! ds this update-fn query))
  ; Pagination not added yet
  (query [this query pagination]
    (if (spec/valid? :clj-storage.spec/only-id-map query)
      (sql/get-by-id ds this (:id query))
      (sql/find-by-keys ds this query)))
  (delete! [this item]
    (sql/delete! ds this item))
  (aggregate [this formula params])
  (add-index [this index unique])
  (expire [this seconds params]))

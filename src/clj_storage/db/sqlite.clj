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
            [clj-storage.db.sqlite.queries :as q]

            [clj-storage.spec]
            [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as ts]
            
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]

            [clj-time.core :as time]
            
            [taoensso.timbre :as log]
            ))


(defrecord SqliteStore [ds table-name]
  Store
  (store! [this item]
    ;; always add a created-at field, in case we need expiration
    (let [item-with-timestamp (assoc item :createdate (java.util.Date.))]
      (sql/insert! (jdbc/get-connection ds) table-name item-with-timestamp)))

  (update! [this query update-fn]
    ;; If it is a prepared statement should be a vector
    (if (spec/valid? ::update-prepared-statement update-fn)
      (jdbc/execute! (jdbc/get-connection ds)
                     [(cond-> "update "
                        true (str 
                              table-name
                              " set "
                              (apply str update-fn)
                              " where ")
                        (spec/valid? ::update-query-vector query) (str (first query))
                        (spec/valid? ::update-query-map query) (str (key query)))
                      (if (spec/valid? ::update-query-vector query)
                        ((partial apply identity) (rest ["GRADE >= ?" 90]))
                        ((partial apply identity) (val query)))])
      (sql/update! ds table-name update-fn query)))

  ;;TODO Pagination not added yet
  (query [this query pagination]
    (if (spec/valid? :clj-storage.spec/only-id-map query)
      (sql/get-by-id (jdbc/get-connection ds) table-name (:id query))
      (if (empty? query)
        (jdbc/execute! ds [(str "select * from " table-name)])
        (sql/find-by-keys ds table-name query))))
  
  (delete! [this item]
    (sql/delete! ds table-name item))
  
  (aggregate [this formula params]
    (spec/assert ::aggregate-params params)
    (jdbc/execute-one! (jdbc/get-connection ds) [(q/aggregate table-name params)]))

  (add-index [this index params]
    (spec/assert ::index-params params)
    (jdbc/execute-one! (jdbc/get-connection ds) [(q/add-index table-name index params)]))

  (expire [this seconds params]
    (spec/assert ::expire-seconds seconds)
    (log/info "Starting a thread to check for expiration for table " table-name)
    ;; The logged-future will return an exception which otherwise would be swallowed till deref
    (log/logged-future
     (while true
       ;; TODO: config extract
       (Thread/sleep 30000)
       (log/debug "Checking for expired rows for table " table-name)
       (storage/delete! this ["CREATEDATE < ?" (log/spy (time/minus (time/now) (time/seconds seconds)))])))))

(defn show-tables [ds]
  (with-open [con (jdbc/get-connection ds)]
  (-> (.getMetaData con) ; produces java.sql.DatabaseMetaData
      (.getTables nil nil `nil (into-array ["TABLE" "VIEW"]))
      (rs/datafiable-result-set ds {}))))

(defn retrieve-table-indices [ds table-name]
  (with-open [con (jdbc/get-connection ds {})]
    (-> (.getMetaData con) ; produces java.sql.DatabaseMetaData
        (.getIndexInfo nil nil table-name true false)
        #_(.getTables nil nil nil (into-array ["TABLE" "VIEW"]))
        (rs/datafiable-result-set ds {}))))

(defn create-sqlite-table [sqlite-ds table-name table-columns]
  (jdbc/execute-one! (jdbc/get-connection sqlite-ds) [(q/create-table table-name table-columns)])
  (SqliteStore. sqlite-ds table-name))

(spec/fdef create-sqlite-table :args (spec/cat
                                      :sqlite-ds ::sqlite-ds
                                      :table-name ::table-name
                                      :table-columns ::table-columns))

;; TODO extract variable
(ts/instrument)
(spec/check-asserts true)

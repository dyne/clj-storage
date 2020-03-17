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
            [clj-storage.config :as conf]

            [clj-storage.spec]
            [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as ts]
            
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]

            [clj-time.core :as time]
            
            [taoensso.timbre :as log]
            ))

(defn- not-empty? [col]
  ((comp not empty?) col))

(defn retrieve-tables [ds]
  (with-open [con (jdbc/get-connection ds {})]
    (-> (.getMetaData con) ; produces java.sql.DatabaseMetaData
        (.getTables nil nil nil (into-array ["TABLE" "VIEW"]))
        (rs/datafiable-result-set ds {}))))

(defn retrieve-table [ds table-name]
  (let [tables
        (with-open [con (jdbc/get-connection ds {})]
          (-> (.getMetaData con) ; produces java.sql.DatabaseMetaData
              (.getTables nil nil nil (into-array ["TABLE" "VIEW"]))
              (rs/datafiable-result-set ds {})))]
    (filter #(= (:sqlite_master/TABLE_NAME %) table-name) tables)))

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

  ;;TODO: Pagination now works only for list all. Maybe do it also for query
  (query [this query pagination]
    (if (spec/valid? ::pagination pagination)
      ;; Order by needed because sql is unordered see https://stackoverflow.com/questions/14468586/efficient-paging-in-sqlite-with-millions-of-records?noredirect=1&lq=1
      (jdbc/execute! ds [(str "select * from " table-name " ORDER BY " (:order-by pagination) " LIMIT " (:limit pagination) " OFFSET " (:offset pagination))])
      (if (spec/valid? :clj-storage.spec/only-id-map query)
        (sql/get-by-id (jdbc/get-connection ds) table-name (:id query))
        (if (empty? query)
          (jdbc/execute! ds [(str "select * from " table-name)])
          (sql/find-by-keys ds table-name query)))))
  
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
     (while (not-empty? (retrieve-table ds table-name))
       (Thread/sleep (log/spy (conf/sqlite-expire-millis (conf/create-config))))
       (log/debug "Checking for expired rows for table " table-name)
       (let [delete-before-timestamp (time/minus (time/now) (time/seconds seconds))]
         (when (and (not-empty? (retrieve-table ds table-name)) (storage/query this ["CREATEDATE < ?" delete-before-timestamp] {}))
           (storage/delete! this ["CREATEDATE < ?" delete-before-timestamp])))))))

(defn count-since [table datetime formula]
  (let [query (str "SELECT * FROM "
                   (:table-name table)
                   " WHERE createdate > ? ")
        formula-part (log/spy (reduce str (map #(str "AND " (name %) " = ? ") (keys (log/spy formula)))))
        end-query (into [] (concat [(str query formula-part ";") datetime]
                                   (vals formula)))]
    (log/info "END QUERY " end-query)
    (sql/query (:ds table) end-query)))

(defn show-tables [ds]
  (with-open [con (jdbc/get-connection ds)]
  (-> (.getMetaData con) ; produces java.sql.DatabaseMetaData
      (.getTables nil nil `nil (into-array ["TABLE" "VIEW"]))
      (rs/datafiable-result-set ds {}))))

(defn retrieve-table-indices [ds table-name]
  (with-open [con (jdbc/get-connection ds {})]
    (-> (.getMetaData con) ; produces java.sql.DatabaseMetaData
        (.getIndexInfo nil nil table-name true false)
        (rs/datafiable-result-set ds {}))))

(defn create-sqlite-table [sqlite-ds table-name table-columns]
  (jdbc/execute-one! (jdbc/get-connection sqlite-ds) [(q/create-table table-name table-columns)])
  (SqliteStore. sqlite-ds table-name))

(spec/fdef create-sqlite-table :args (spec/cat
                                      :sqlite-ds ::sqlite-ds
                                      :table-name ::table-name
                                      :table-columns ::table-columns))

(defn create-sqlite-tables [sqlite-ds name-param-m name-columns-m]
  (let [tables (atom {})]
    (doall (map #(let [table-name (key %)
                       columns (val %)
                       params (get name-param-m table-name)
                       storage (create-sqlite-table sqlite-ds table-name columns)]
                   (swap! tables (fn [t] (assoc t table-name storage)))
                   (when (:expireAfterSeconds params)
                     (storage/expire storage (:expireAfterSeconds params) {})))
                name-columns-m))
    @tables))

(when (conf/spec-instrument (conf/create-config))
  (ts/instrument))

(spec/check-asserts (conf/spec-asserts (conf/create-config)))

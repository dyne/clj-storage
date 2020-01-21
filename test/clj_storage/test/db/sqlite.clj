;; clj-storage - a minimal storage library

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

(ns clj-storage.test.db.sqlite
  (:require [midje.sweet :refer [against-background before after facts fact => truthy]]

            [clj-storage.core :as storage]
            [clj-storage.db.sqlite :as db]
            [clj-storage.test.db.sqlite.test-db :as test-db]
            
            [taoensso.timbre :as log]))

(def headers [:name :appearance :cost :grade])
(def main-table-name "FRUIT")
(def main-columns ["ID INTEGER PRIMARY KEY AUTOINCREMENT"
                   "NAME VARCHAR(32) UNIQUE"
                   "APPEARANCE VARCHAR(32) DEFAULT NULL"
                   "COST INT DEFAULT NULL"
                   "GRADE REAL DEFAULT NULL"
                    "CREATEDATE TIMESTAMP NOT NULL"])
(def secondary-table-name "CLASSIFICATION")
(def secondary-columns ["NAME VARCHAR(32) UNIQUE"
                        "GENUS VARCHAR(32)"
                        "CREATEDATE TIMESTAMP NOT NULL"])


(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]

                    (let [fruit-store (db/create-sqlite-table (test-db/get-datasource) main-table-name main-columns)
                          classification-store (db/create-sqlite-table (test-db/get-datasource) secondary-table-name secondary-columns)]

                      (facts "Test the sqlite protocol implemetation"
                             (fact "Table(s) created"
                                   (let [tables (db/show-tables (test-db/get-datasource))]
                                     (count tables) => 2
                                     (-> tables first :sqlite_master/TABLE_NAME) => secondary-table-name
                                     (-> tables second :sqlite_master/TABLE_NAME) => "FRUIT"))

                             (fact "Add index"
                                   (storage/add-index fruit-store
                                                      "idx_fruit_name"
                                                      {:unique true
                                                       :columns "NAME"})
                                   (-> (filter
                                        (comp #(= % "idx_fruit_name") :INDEX_NAME)
                                        (db/retrieve-table-indices (test-db/get-datasource) main-table-name))
                                       first
                                       :COLUMN_NAME)
                                   => "NAME")

                             ;; TODO JOINS
                             #_(fact "Check join between two tables"
                                   )
                             
                             (fact "Insert rows to table"
                                   (storage/store! fruit-store (zipmap headers ["Apple" "red" 59 nil])) => truthy
                                   (storage/store! fruit-store (zipmap headers ["Banana" "yellow" nil 92.2])) => truthy
                                   (storage/store! fruit-store (zipmap headers ["Peach" nil 139 90.0])) => truthy
                                   (storage/store! fruit-store (zipmap headers ["Orange" "juicy" 89 88.6])) => truthy
                                   (storage/store! fruit-store (zipmap headers ["Cherry" "red" nil nil])) => truthy
                                   (storage/store! classification-store {:name "Banana" :genus "Musa"})
                                   (storage/store! classification-store {:name "Orange" :genus "Citrus"})
                                   ;; Add expiration for classification-store
                                   (storage/expire classification-store 30 {}))

                             (fact "Query the table with pagination"
                                   (count (storage/query fruit-store {} {})) => 5
                                   (count (storage/query classification-store {} {})) => 2
                                   (count (storage/query fruit-store {:id 1} {})) => 1
                                   (:FRUIT/NAME (first (storage/query fruit-store {:id 1} {}))) => "Apple"
                                   (count (storage/query fruit-store {:FRUIT/APPEARANCE "red"} {})) => 2
                                   (count (storage/query fruit-store ["COST > ?" 80] {})) => 2
                                   (count (storage/query fruit-store ["APPEARANCE is null"] {})) => 1
                                   (count (storage/query fruit-store ["CREATEDATE > ?" (java.util.Date. "January 1, 1970, 00:00:00 GMT")] {})) => 5)

                             (fact "Test the aggregates"
                                   (vals (storage/aggregate fruit-store nil {:select "COUNT (*)"})) => '(5)
                                   (vals (storage/aggregate fruit-store nil {:select "COUNT (DISTINCT APPEARANCE)"})) => '(3)
                                   (vals (storage/aggregate fruit-store nil {:select "MAX (COST)"})) => '(139))

                             (fact "Update some rows"
                                   (storage/query fruit-store {:FRUIT/APPEARANCE "peach color"} {}) => []
                                   (storage/update! fruit-store {:FRUIT/NAME "Peach"} {:FRUIT/APPEARANCE "peach color"}) => {:next.jdbc/update-count 1}
                                   (-> (storage/query fruit-store {:FRUIT/APPEARANCE "peach color"} {})
                                       first
                                       (dissoc :FRUIT/CREATEDATE)) => {:FRUIT/APPEARANCE "peach color"
                                                                      :FRUIT/COST 139
                                                                      :FRUIT/GRADE 90.0
                                                                      :FRUIT/ID 3
                                                                      :FRUIT/NAME "Peach"}
                                   
                                   (count (storage/query fruit-store ["GRADE >= ?" 90] {})) => 2
                                   (count (storage/query fruit-store ["GRADE >= ?" 100] {})) => 0
                                   (storage/update! fruit-store ["GRADE >= ?" 90] "GRADE = ( 50 + grade )") => [{:next.jdbc/update-count 2}]
                                   (-> (storage/query fruit-store {:FRUIT/NAME "Peach"} {})
                                       first
                                       (dissoc :FRUIT/CREATEDATE))
                                   => {:FRUIT/APPEARANCE "peach color"
                                       :FRUIT/COST 139
                                       :FRUIT/GRADE 140.0
                                       :FRUIT/ID 3
                                       :FRUIT/NAME "Peach"}
                                   (count (storage/query fruit-store ["GRADE >= ?" 100] {})) => 2)
                             
                             (fact "Delete some rows"
                                   (vals (storage/aggregate fruit-store nil {:select "COUNT (*)"})) => '(5)
                                   (storage/delete! fruit-store ["COST > ?" 0]) => {:next.jdbc/update-count 3}
                                   (vals (storage/aggregate fruit-store nil {:select "COUNT (*)"})) => '(2)))

                      (facts "Test the expiration" :slow
                             (count (storage/query classification-store {} {})) => 2
                             (Thread/sleep (* 40 1000))
                             (count (storage/query classification-store {} {})) => 0
                             (storage/store! classification-store {:name "Orange" :genus "Citrus"})
                             (count (storage/query classification-store {} {})) => 1)))

;; clj-storage - Minimal storage library based on an abstraction which helps seamlessly switching between DBs

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

(ns clj-storage.test.db.sqlite.test-db
  (:require [clj-storage.db.sqlite.queries :as q]
            
            [next.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(def db {:dbtype "sqlite" :dbname "/tmp/test.db"})

(def ds (atom nil))

(defn get-test-db-connection []
  (jdbc/get-connection @ds))

(defn get-datasource []
  @ds)

(defn setup-db []
  (log/debug "Setting a file system sqlite DB")
  (reset! ds (jdbc/get-datasource db)))

(defn teardown-db []
  (log/debug "Tearing down test DB " @ds)
  (jdbc/execute-one! (get-test-db-connection) [(q/drop-table "FRUIT")])
  (jdbc/execute-one! (get-test-db-connection) [(q/drop-table "CLASSIFICATION")])
  (reset! ds nil))

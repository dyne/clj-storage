;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; With contributions by
;; Duncan Mortimer <dmortime@thoughtworks.com>

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

(ns clj-storage.test.db.mongo.test-db
  (:require [clj-storage.db.mongo :as m]
            [taoensso.timbre :as log]))

(def test-db-name "test-db")
(def test-db-uri (format "mongodb://localhost:27017/%s" test-db-name))

(def db-and-conn (atom {}))

(defn get-test-db []
  (:db @db-and-conn))

(defn get-test-db-connection []
  (:conn @db-and-conn))

(defn setup-db []
  (log/debug "Setting up test DB")
  (->> (m/get-mongo-db-and-conn test-db-uri)
       (m/drop-db)
       (reset! db-and-conn)))

(defn teardown-db []
  (log/debug "Tearing down test DB " @db-and-conn)
  (m/drop-db @db-and-conn)
  (m/disconnect (get-test-db-connection))
  (reset! db-and-conn nil))

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

(ns clj-storage.test.db.redis.test-db
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :as log]))

(def uri "redis://127.0.0.1:6379")
(def server1-conn {:pool {} :spec {:uri uri}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(wcar* (car/ping))

(def db-and-conn (atom {}))

(defn get-test-db []
  (:db @db-and-conn))

(defn get-test-db-connection []
  (:conn @db-and-conn))

#_(defn setup-db []
  (log/debug "Setting up REDIS test DB")
  (->> (m/get-mongo-db-and-conn test-db-uri)
       (m/drop-db)
       (reset! db-and-conn)))

#_(defn teardown-db []
  (log/debug "Tearing down test DB " @db-and-conn)
  (m/drop-db @db-and-conn)
  (m/disconnect (get-test-db-connection))
  (reset! db-and-conn nil))

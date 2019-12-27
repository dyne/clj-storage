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
            [clj-storage.db.redis :refer [wcar* create-redis-database]]
            [cheshire.core :as json]
            [taoensso.timbre :as log]))

;; Redis holds by default up to 16 dbs. First one here is indicated by the /0
(def uri "redis://127.0.0.1:6379/0")

(def store-and-conn (atom {}))

(defn get-test-store []
  (:store @store-and-conn))

(defn get-test-connection []
  (:conn @store-and-conn))

(defn setup-db []
  (log/debug "Connecting to REDIS test DB")
  (reset! store-and-conn (create-redis-database uri))
  (log/debug  "Testing connction to redis: " (wcar* (get-test-connection) (car/ping))))

(defn teardown-db []
  ;; TODO: this still doesnt kill the client (returns 0)
  (let [client (log/spy (wcar* (get-test-connection) (car/client-list)))
        client-id (log/spy (-> client (clojure.string/split #"=") second (clojure.string/split #" ") first))]
    (wcar* (get-test-connection) (car/client-kill "ID" client-id))))

;; clj-storage - Minimal storage library based on an abstraction which helps seamlessly switching between DBs

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2019- Dyne.org foundation

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

(ns clj-storage.db.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [clojure.spec.alpha :as s]
            [clj-storage.core :as storage :refer [Store]]
            [taoensso.timbre :as log]))

(defmacro wcar* [conn & body] `(car/wcar ~@conn ~@body))

(defrecord RedisStore [redis-conn]
  Store
    (wcar* (:conn this) (car/set (::key item) (::value item))))
  (store! [database item]
  
  (update! [database q update-fn]
    )

    (wcar* (:conn this) (car/get (log/spy (::key query)))))
  (query [database query pagination]
  
    )
  (delete! [database item]

  (aggregate [this formula params]
    )

  (add-index [database index unique])

  (expire [database seconds]))

(defn create-redis-store [uri]
  (let [conn {:pool {} :spec {:uri uri}}]
    {:store (RedisStore. conn)
     :conn conn}))

;; clj-storage - Minimal storage library based on an abstraction which helps seamlessly switching between DBs

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


(ns clj-storage.db.sqlite.queries
  (:require [taoensso.timbre :as log]))

(defn create-table [name columns]
  (let [columns-str (loop [s ""
                           c 0]
                      (if (= c (- (count columns) 1))
                        (str s (nth columns c))
                        (recur (str s (nth columns c) ",") (inc c))))]
    (str "CREATE TABLE " name " "
         "(" columns-str ")")))

(defn drop-table [name]
  (str "DROP TABLE " name))

(defn aggregate [table-name params]
  (cond-> "SELECT "
    (:select params) (str (:select params) " ")
    true (str "FROM " table-name)
    (:group-by params) (str " GROUP BY " (:group-by params))
    (:order-by params) (str  "ORDER BY " (:order-by params))
    true (str ";")))

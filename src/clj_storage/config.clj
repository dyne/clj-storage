;; clj-storage - a minimal storage library

;; Copyright (C) 2020- Dyne.org foundation

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

(ns clj-storage.config
  (:require [environ.core :as env]
            [taoensso.timbre :as log]))

(def env-vars #{:spec-asserts :redis-atomic-retries :sqlite-expire-millis})

(defn create-config []
  (select-keys env/env env-vars))

(defn get-env
  "Like a normal 'get' except it also ensures the key is in the env-vars set"
  ([config-m key]
   (get config-m (env-vars key)))
  ([config-m key default]
   (get config-m (env-vars key) default)))

(defn redis-atomic-retries [config-m]
  (Integer. (get-env config-m :redis-atomic-retries "100")))

(defn sqlite-expire-millis [config-m]
  (Integer. (get-env config-m :sqlite-expire-millis "30000")))

(defn spec-asserts [config-m]
  (not (= "false" (get-env config-m :spec-asserts "false"))))

(defn spec-instrument [config-m]
  (not (= "false" (get-env config-m :spec-instrument "false"))))

(defn pagination-max-per-page [config-m]
  (Integer. (get-env config-m :pagination-max-per-page "100")))

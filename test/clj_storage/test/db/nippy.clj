(ns clj-storage.test.db.nippy
  (:require [midje.sweet :refer :all]
            [clj-storage.core :refer :all]
            [clj-storage.db.nippyfs :refer :all]
            [taoensso.nippy :as nippy]
            [taoensso.timbre :as log]))

(def nip (create-nippy-store {:path "/tmp/clj-storage-test-nippyfs"
                              :prefix ""
                              :suffix ".dat"}))

(def stress-data [[{:a 1 :b 2 :c 3} {:a 4 :b 5 :c {:z 7 :y 9}}] [:a :b]])

(fact "Test write, read, update and delete with NippyFS"
      (store!  nip "stress-data" stress-data)
      (fetch   nip "stress-data") => stress-data
      (update! nip "stress-data" last)
      (fetch   nip "stress-data") => [:a :b]
      (delete! nip "stress-data")
      (delete-all! nip)
;;      (delete! nip "stress-data")
      )

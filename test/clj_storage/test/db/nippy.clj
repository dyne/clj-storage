(ns clj-storage.test.db.nippy
  (:require [midje.sweet :refer [fact =>]]
            [clj-storage.core :refer [store! fetch update! delete! query  delete-all!]]
            [clj-storage.db.nippyfs :refer [create-nippy-store]]
            [taoensso.timbre :as log]))

(def nip (create-nippy-store {:path "/tmp/clj-storage-test-nippyfs"
                              :prefix ""
                              :suffix ".dat"}))

(def stress-data [[{:a 1 :b 2 :c 3} {:a 4 :b 5 :c {:z 7 :y 9}}] [:a :b]])

(fact "NippyFS tests"
      (fact "simple write, read, update and delete"
            (store!  nip "stress-data" stress-data)
            (fetch   nip "stress-data") => stress-data
            (update! nip "stress-data" last)
            (fetch   nip "stress-data") => [:a :b]
            (delete! nip "stress-data")            
            ;;      (delete! nip "stress-data")
            )

      (fact "query over a few entries"
            (store! nip "query-test-chosen-one" stress-data)
            (store! nip "query-test-two" stress-data)
            (store! nip "query-test-chosen-three" stress-data)
            (store! nip "query-test-four" stress-data)
            (store! nip "query-test-chosen-five" stress-data)
            (query nip "-chosen-") => [[[{:a 1, :b 2, :c 3} {:a 4, :b 5, :c {:z 7, :y 9}}] [:a :b]]
                                       [[{:a 1, :b 2, :c 3} {:a 4, :b 5, :c {:z 7, :y 9}}] [:a :b]]
                                       [[{:a 1, :b 2, :c 3} {:a 4, :b 5, :c {:z 7, :y 9}}] [:a :b]]]
            )
      (delete-all! nip)
      )

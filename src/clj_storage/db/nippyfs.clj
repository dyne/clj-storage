;; clj-storage - a minimal storage library

;; Copyright (C) 2017-2018 Dyne.org foundation

;; Implementation designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; Distributed under the Eclipse Public License either version 1.0
;; (same as Clojure) or (at your option) any later version.

(ns clj-storage.db.nippyfs
  (:require
   [clojure.java.io :as io :refer
    [file delete-file input-stream output-stream]]
   [clojure.string :refer [blank?]]
   [clj-storage.core :refer [Store]]
   [taoensso.nippy :as nippy :refer [freeze-to-out! thaw-from-in!]])
  (:import [java.io DataInputStream DataOutputStream]))

;; implements a simple filesystem based storage solution
;; using keys as filenames and storing contents with nippy
;; the base object stores the path to a directory where all
;; keys (files) are stored and the nippy configuration.

(defn- getkey [n k]
  {:pre [(coll? n) (string? k)]
   :post [(string? %)]}
  (let [prefix (:prefix n)
        suffix (:suffix n)]
    (str (:path n) "/"
         (if (blank? prefix) "" prefix)
         k ;; TODO: sanitise filename
         (if (blank? suffix) "" suffix))))

(defn- readkey [n k]
  {:pre [(coll? n) (string? k)]}
  (with-open [r (io/input-stream (getkey n k))]
    (nippy/thaw-from-in! (DataInputStream. r))))

(defn- writekey [n k v]
  {:pre [(coll? n) (string? k) (some? v)]}
  (with-open [w (io/output-stream (getkey n k))]
    (nippy/freeze-to-out! (DataOutputStream. w) v)))

(defn- delete-files-recursively [fname & [silently]]
  {:pre [(string? fname)]}
  ;; safety measure
  (if (some #(= fname %) ["/" "/usr" "/home" "/lib" "/bin" "/sbin"
                          "/var" "/boot" "/mnt" "/srv" "/etc" "/media"])
    (throw (Exception. "Refusing to delete system directory"))
    (letfn [(delete-f [file]
              (when (.isDirectory file)
                (doseq [child-file (.listFiles file)]
                  (delete-f child-file)))
              (io/delete-file file silently))]
      (delete-f (io/file fname)))))

(defn- list-files-matching
  "returns a sequence of files found in a directory whose names match
  a regexp"
  [directory regex]
  {:pre [(string? directory) (some? regex)]}
  (let [dir   (io/file directory)
        files (file-seq dir)]
    (remove nil?
            (map #(let [f (.getName %)]
                    (if (re-find regex f) %)) files))))

(defrecord NippyFS [conf]

  ;; Configuration example:
  ;; {:path   "/tmp/nippyfs"
  ;;  :prefix ""
  ;;  :suffix ".dat" }

  Store

  (store! [this k item] (writekey conf k item))

  (update! [this k ufn]
    (->> (readkey conf k)
         ufn ;; takes a single arg
         (writekey conf k)))

  (fetch [this k] (readkey conf k))

  (query [this query]
    (loop [[f & files]
           ;; TODO: may add caching here for speed
           (list-files-matching
            (:path conf)
            (java.util.regex.Pattern/compile query))
           res []]
      (let [entry
            (with-open [r (io/input-stream f)]
              (nippy/thaw-from-in! (DataInputStream. r)))]
        (if (empty? files) (if (nil? entry) res (conj res entry))
            (recur  files (if (nil? entry) res (conj res entry)))))))

  (delete! [this k] (io/delete-file (getkey conf k)))

  (delete-all! [this]
    (delete-files-recursively (:path conf)))

  ;; TODO: aggregate functions
  )

(defn create-nippy-store [conf]
  (io/make-parents (str (:path conf) "/make-parents"))
  (NippyFS. conf))

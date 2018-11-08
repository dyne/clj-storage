# clj-storage - minimal abstract database storage lib

<a href="https://www.dyne.org"><img
	src="https://secrets.dyne.org/static/img/swbydyne.png"
		alt="software by Dyne.org"
			title="software by Dyne.org" class="pull-right"></a>

[![Build Status](https://travis-ci.org/Commonfare-net/clj-storage.svg?branch=master)](https://travis-ci.org/Commonfare-net/clj-storage)

This library is a minimalist clojure protocol abstraction over
document databases. The only implementation so far is MongoDB, but can
be extended in the future as needed. The main goal is to make it
possible for server-side applications using this library to change the
storage database without changing their code.

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.dyne/clj-storage.svg)](https://clojars.org/org.clojars.dyne/clj-storage)

Here below is the abstract protocol with all functions that may be implemented to support a database implementation:

```clj
(defprotocol Store
  (store! [e k item]
    "Store item against the key k")
  (store-and-create-id! [e item]
    "Store item and return with :_id created by db")
  (update! [e k update-fn]
    "Update the item found using key k by running the update-fn on it and storing it")
  (fetch [e k]
    "Retrieve item based on primary id")
  (query [e query]
    "Items are returned using a query map")
  (list-per-page [e query page per-page]
    "List all items in a collection using pagination. Per page is the number of items per page and page is the number of page. Items are sorted by _id")
  (delete! [e k]
    "Delete item based on primary id")
  (delete-all! [e]
    "Delete all items from a coll")
  (aggregate [e formula]
    "Process data records and return computed results")
  (count-since [e date-time formula]
    "Count the number of documents that since a date-time and after applying a formula. {} for an empty formula. This is meant only for collections that contain a `created-at` field.")
  (count* [e params]
    "Count the number of documents after applying a formula. {} for an empty formula."))   
```


## License

This Free and Open Source research and development activity is funded
by the European Commission in the context of Collective Awareness
Platforms for Sustainability and Social Innovation (CAPSSI) grant nr.687922.

The clj-storage library is Copyright (C) 2017-2018 by the Dyne.org Foundation, Amsterdam

Designed, written and maintained by Aspasia Beneti <aspra@dyne.org>

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

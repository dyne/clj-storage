# clj-storage - minimal abstract database storage lib

This library is a minimalist clojure protocol abstraction over databases. The available implementations so far are Redis, MongoDB and sqlite, but can be extended in the future as needed. The main goal is to make it possible for server-side applications using this library to witch databases without having to change much in their code.

<a href="https://www.dyne.org"><img
	src="https://secrets.dyne.org/static/img/swbydyne.png"
		alt="software by Dyne.org"
			title="software by Dyne.org" class="pull-right"></a>

[Protocol](#Protocol) | [Implementations](#Implementations) | [Running the tests](#Running-the-tests) | [Todos](#Todos) | [Acknowledgements](#Acknowledgements) | [License](#License) | [change log](https://github.com/Commonfare-net/clj-storage/blob/feature/full-abstraction/CHANGELOG.markdown)
[![Build Status](https://travis-ci.org/Commonfare-net/clj-storage.svg?branch=master)](https://travis-ci.org/Commonfare-net/clj-storage)
[![Clojars Project](https://img.shields.io/clojars/v/socia)](https://clojars.org/org.clojars.dyne/clj-storage)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

## Protocol

Here below is the abstract protocol with all functions that may be implemented to support a database implementation:

```clj
(defprotocol Store
  (store! [s item]
    "Store an item to storage s")
  (update! [s query update-fn]
    "Update all items found by with the update-fn, specific to the implementation")
  (query [s query pagination]
    "Find one or more items given a query map (does a fetch when query map is only id). Pagination will be used if not empty")
  (delete! [s item]
    "Delete item from a storage s")
  (aggregate [s formula params]
    "Process data aggregate and return computed results")
  (add-index [s index params]
    "Add an index to a storage s. The map parameter can differ per db implementation")
  (expire [s seconds params]
    "Expire items of this storage after seconds"))
```
## Implementation

All implementations can be found under src/clj_storage/db. All implementations add the field _created-at_ so it can be used for expiration. The protocol is implemented for every db, however since there is one protocol with a number of DB paradigms, there are slight differences in each implementation. In particular:

### Redis
- The core Redis Key Value functionality is implemented
- Since paging on redis is available only for paricular types, it is currently not supported
- Some particular to Redis functions are available, namely: count-keys , get-all-keys, count-sorted-set.

### Mongo
- Some particular to Mongo functions are available, namely: count-items, count-since.

### SQLite
- The SQLite implementation deals with expiration programmatically
- Pagination works for list-all but not for query atm
- Some particular to Mongo functions are available, namely: show-tables, retrieve-table-indices.

## Running the tests

To run all tests one need to run
` lein midje`
on the project dir

#### Run only the fast tests

Some of the tests are marked as slow. If you want to avoid running them you can either

```
lein midje :filter -slow
```

or use the alias

```
lein test-basic
```

## Todos

- In-memory implementation for testing

## Acknowledgements

The Social Wallet API is Free and Open Source research and development
activity funded by the European Commission in the context of
the
[Collective Awareness Platforms for Sustainability and Social Innovation (CAPSSI)](https://ec.europa.eu/digital-single-market/en/collective-awareness) program. Social
Wallet API uses the
underlying [Freecoin-lib](https://github.com/dyne/freecoin-lib)
blockchain implementation library and adopted as a component of the
social wallet toolkit being developed for
the [Commonfare project](http://pieproject.eu) (grant nr. 687922) .


## License

This project is licensed under the AGPL 3 License - see the [LICENSE](LICENSE) file for details

#### Additional permission under GNU AGPL version 3 section 7.

If you modify Freecoin-lib, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

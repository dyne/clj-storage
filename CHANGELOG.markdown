# Changelog

## clj-storage 0.10.0
* Small fixes especially the disconnect db function, discovered when testing the social wallet GUI. Also added linter conf
	
## clj-storage 0.9.0
* Release including the API KEY option for requests

## clj-storage 0.8.0
* Stable release: added count start with date and other filtering
	
## clj-storage 0.7.0
* Added pagination

## clj-storage 0.6.0
* Added count-since for throttling behaviour in auth lib
	
## clj-storage 0.5.1
* Updated the mongo dependencies to the latest SNAPSHOT version in order to use Decimal128 high precision numeric representation

## clj-storage 0.5.0
* Moved to the storage lib test helper function as part of the auth lib separation from the SWAPI
	
## clj-storage 0.3.0
* Initial stable version of the storage lib, implementing in-memory and MongoDB including expiration

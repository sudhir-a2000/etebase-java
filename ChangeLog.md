# Changelog

## Version 2.3.2
* Fix crash for Android devices older than Android 7 due to flapigen issue

## Version 2.3.1
* Fix issue with custom urls lacking a trailing slash
* Update etebase dependency

## Version 2.3.0
* Fix crashes on Lineage for MicroG which was having issue with OptionalLong.
* Remove usages of Optional throughout the lib (was never exposed in the API).
* Remove library desugaring (we can do it now that we no longer use Optional).

## Version 2.2.0
* Collection manager list: support passing arrays of collection types

## Version 2.1.0
* Login: automatically init the account if not init
* Have global and immutable collection types (changes the create and list APIs)
* Update etebase dependency

## Version 2.0.0
* Just a version bump for packaging

## Version 0.2.2
* Expose FileSystemCache class for caching etebase object on the FS
* Update etebase dependency

## Version 0.2.1
* Expose fromUsername for invitations
* Update etebase dependency

## Version 0.2.0
* Use new and shorter fingerprint pretty format
* Update etebase dependency

## Version 0.1.4
* Expose access level values as an enum
* Update etebase dependency

## Version 0.1.3
* Update etebase dependency
* HttpException: don't inherit from ConnectionException.
* Item revisions: fix naming of item revisions response functions.
* Add an API function to check if it's an etebase server.
* Remove usage of optional long from our API
* Change markDeleted() to delete()
* Rename getDefaultApiUrl to getDefaultServerUrl.

## Version 0.1.2
* Update etebase dependency
* Clean up exception types

## Version 0.1.1
* Fix getting item revisions
* Add convenience functions around member, invitation and revision listing

## Version 0.1.0
* Initial release

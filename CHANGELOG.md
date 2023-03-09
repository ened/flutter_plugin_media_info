## 0.12.0+2

* Licensing info + formatting corrected

## 0.12.0+1

* Add missing changelog for 0.12.0:
  * Fix Issue #12, a performance issue when using the package at scale
  * Extend the test suite to include integration tests and CI/CD

## 0.12.0

* Fix Issue #12, a performance issue when using the package at scale
* Extend the test suite to include integration tests and CI/CD

## 0.11.1

* Android: Require `compileSdkVersion 32`

## 0.11.0

* Android: **BREAKING CHANGE** Plugin now depends on ExoPlayer 2.18.1

## 0.10.0

* Migrate project for Flutter 2.10
* Fixed a few issues regarding screenshot accuracy for videos. Thank you @psimoes93

## 0.9.0

* Android: **BREAKING CHANGE** Plugin now depends on ExoPlayer 2.15.0
* Android: Resolves a issue which may cause a deadlock when reading media information.

## 0.8.0

* Android: **BREAKING CHANGE** Plugin now depends on ExoPlayer 2.14.2

## 0.7.0

* Support null-safety

## 0.6.5

* Resolve issue when a image info completer may be resolved twice
* Specify a time to generate the thumbnail from
* Generate thumbnails from network videos
* Updated example app to include image from network video

## 0.6.4

* Resolve issue when a image info completer may be resolved twice

## 0.6.3

* Resolve iOS issues when reading audio tracks

## 0.6.2

* Image decoding moved to the Dart side
* Add support for audio details
* Fixes bugs when decoding video on Android

## 0.6.1

* Android: Removes logging in the main plugin
* Android: Removes various unsafe/deprecated method calls

## 0.6.0

* Update pubspec.yaml format
* Android: Video rotation can not be detected on Android < 17
* Support the v2 Android embedder
* Bump a few example dependency versions
* iOS: Remove unused logging
* iOS: Migrate iOS project
* Adds basic method plugin tests on Dart side
* Resolves width / height detection for portrait videos. Thank you @bdoubleu86

## 0.5.2

* Android: Optimize the logic when a ExoPlayer frame is actually ready.

## 0.5.1

* Android: Ensure underlying ExoPlayer instance is reused & run on a single thread
  for all operations.

## 0.5.0

* Android: Switch Metadata retrieval from MediaStore methods to ExoPlayer
* Android: Bump AGP & Gradle 

## 0.4.1

* Updates AGP to latest
* Bump Glide dependency for Android

## 0.4.0

* Replace Kotlin with Java 
* Various bugfixes

## 0.3.0

* Ensure Android results are always submitted on the main thread

## 0.2.2+1

* Do not return invalid values when detection fails. Throw flutter errors instead.

## 0.2.2

* iOS: Read mimeType
* iOS: Detect image width & height

## 0.2.1

* Prevent crashes on iOS when asset track list is empty.

## 0.2.0

* Require object instance to instead of providing static methods. This improves
  mocking & testing capabilities.

## 0.1.0

* getMediaInfo implementation for iOS
* generateThumbnail implementation for iOS
* improve generateThumbnail API to return a Future<String> with error cases
  instead of a boolean future.

## 0.0.4+1

* Bump gradle plugin version

## 0.0.4

* Set Android compile SDK version to 28
* Migrate to AndroidX

## 0.0.3+1

* Upgrade gradle & kotlin dependencies

## 0.0.3

* [Android] Thumbnail support for video / image files

## 0.0.2

* Detect mimeType

## 0.0.1

* Initial Release with Android platform support.

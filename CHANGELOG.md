## next

* Bump a few example dependency versions
* iOS: Remove unused logging
* iOS: Migrate iOS project
* Adds basic method plugin tests on Dart side

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

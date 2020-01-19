# media_info

[![Pub](https://img.shields.io/pub/v/media_info.svg)](https://pub.dartlang.org/packages/media_info)

Utilizes platform code to determine audio, video & photo properties.

Depending on the underlying platform version, various workarounds will be applied to fetch the fields.

Currently supported properties:

* width
* height
* frame rate
* duration (in milliseconds)
* mime type
* number of tracks

Additionally, thumbnails can be generated.

## Integration tests

E2E tests are currently WIP.

They can be tested in the example folder by running the following command:

```
$ flutter drive test_driver/media_info_e2e.dart
```

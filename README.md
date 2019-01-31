# media_info

[![Build Status](https://travis-ci.org/ened/flutter_plugin_media_info.svg?branch=master)](https://travis-ci.org/ened/flutter_plugin_media_info)
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

Additionally, video thumbnails can be generated.

## Implementation Status

- [x] Video Support (*Android*)
- [ ] Video Support (*iOS*)
- [ ] Audio Support
- [ ] Photo Support
- [ ] Thumbnails (*Android only*)
- [ ] Integration tests & CI

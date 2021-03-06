// Mocks generated by Mockito 5.0.4 from annotations
// in media_info/example/ios/.symlinks/plugins/media_info/test/media_info_test.dart.
// Do not manually edit this file.

import 'dart:async' as _i5;

import 'package:flutter/src/services/binary_messenger.dart' as _i3;
import 'package:flutter/src/services/message_codec.dart' as _i2;
import 'package:flutter/src/services/platform_channel.dart' as _i4;
import 'package:mockito/mockito.dart' as _i1;

// ignore_for_file: comment_references
// ignore_for_file: unnecessary_parenthesis

class _FakeMethodCodec extends _i1.Fake implements _i2.MethodCodec {}

class _FakeBinaryMessenger extends _i1.Fake implements _i3.BinaryMessenger {}

/// A class which mocks [MethodChannel].
///
/// See the documentation for Mockito's code generation for more information.
class MockMethodChannel extends _i1.Mock implements _i4.MethodChannel {
  MockMethodChannel() {
    _i1.throwOnMissingStub(this);
  }

  @override
  String get name =>
      (super.noSuchMethod(Invocation.getter(#name), returnValue: '') as String);
  @override
  _i2.MethodCodec get codec => (super.noSuchMethod(Invocation.getter(#codec),
      returnValue: _FakeMethodCodec()) as _i2.MethodCodec);
  @override
  _i3.BinaryMessenger get binaryMessenger =>
      (super.noSuchMethod(Invocation.getter(#binaryMessenger),
          returnValue: _FakeBinaryMessenger()) as _i3.BinaryMessenger);
  @override
  _i5.Future<T?> invokeMethod<T>(String? method, [dynamic arguments]) =>
      (super.noSuchMethod(Invocation.method(#invokeMethod, [method, arguments]),
          returnValue: Future<T?>.value(null)) as _i5.Future<T?>);
  @override
  _i5.Future<List<T>?> invokeListMethod<T>(String? method,
          [dynamic arguments]) =>
      (super.noSuchMethod(
          Invocation.method(#invokeListMethod, [method, arguments]),
          returnValue: Future<List<T>?>.value(<T>[])) as _i5.Future<List<T>?>);
  @override
  _i5.Future<Map<K, V>?> invokeMapMethod<K, V>(String? method,
          [dynamic arguments]) =>
      (super.noSuchMethod(
              Invocation.method(#invokeMapMethod, [method, arguments]),
              returnValue: Future<Map<K, V>?>.value(<K, V>{}))
          as _i5.Future<Map<K, V>?>);
  @override
  void setMethodCallHandler(
          _i5.Future<dynamic> Function(_i2.MethodCall)? handler) =>
      super.noSuchMethod(Invocation.method(#setMethodCallHandler, [handler]),
          returnValueForMissingStub: null);
  @override
  bool checkMethodCallHandler(
          _i5.Future<dynamic> Function(_i2.MethodCall)? handler) =>
      (super.noSuchMethod(Invocation.method(#checkMethodCallHandler, [handler]),
          returnValue: false) as bool);
  @override
  void setMockMethodCallHandler(
          _i5.Future<dynamic>? Function(_i2.MethodCall)? handler) =>
      super.noSuchMethod(
          Invocation.method(#setMockMethodCallHandler, [handler]),
          returnValueForMissingStub: null);
  @override
  bool checkMockMethodCallHandler(
          _i5.Future<dynamic> Function(_i2.MethodCall)? handler) =>
      (super.noSuchMethod(
          Invocation.method(#checkMockMethodCallHandler, [handler]),
          returnValue: false) as bool);
}

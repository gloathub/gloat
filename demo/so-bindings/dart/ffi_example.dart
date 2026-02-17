import 'dart:ffi';
import 'dart:io';
import 'package:ffi/ffi.dart';

typedef FactorialC = Int64 Function(Int64);
typedef FactorialDart = int Function(int);

typedef StrToStrC = Pointer<Utf8> Function(Pointer<Utf8>);
typedef StrToStrDart = Pointer<Utf8> Function(Pointer<Utf8>);

typedef RepeatStringC = Pointer<Utf8> Function(Pointer<Utf8>, Int64);
typedef RepeatStringDart = Pointer<Utf8> Function(Pointer<Utf8>, int);

typedef ShoutItC = Void Function(Pointer<Utf8>);
typedef ShoutItDart = void Function(Pointer<Utf8>);

typedef MaybeC = Int32 Function();
typedef MaybeDart = int Function();

void main() {
  final lib = DynamicLibrary.open('../example.so');

  final factorial =
      lib.lookupFunction<FactorialC, FactorialDart>('factorial');
  final greet =
      lib.lookupFunction<StrToStrC, StrToStrDart>('greet');
  final repeatString =
      lib.lookupFunction<RepeatStringC, RepeatStringDart>('repeat_string');
  final shoutIt =
      lib.lookupFunction<ShoutItC, ShoutItDart>('shout_it');
  final maybe =
      lib.lookupFunction<MaybeC, MaybeDart>('maybe');
  final sortJsonArray =
      lib.lookupFunction<StrToStrC, StrToStrDart>('sort_json_array');

  for (int n = 1; n <= 10; n++) {
    print('$n! = ${factorial(n)}');
  }

  print(greet('World'.toNativeUtf8()).toDartString());
  print(repeatString('ha'.toNativeUtf8(), 3).toDartString());

  shoutIt('hello from yamlscript'.toNativeUtf8());

  final m = maybe();
  print('maybe: ${m != 0 ? "true" : "false"}');

  print('sorted: ${sortJsonArray('[3,1,4,1,5,9,2,6]'.toNativeUtf8()).toDartString()}');
}

program ffi_example;

{$mode objfpc}{$H+}

uses
  ctypes;

function factorial(n: cint64): cint64; cdecl; external 'example.so';
function greet(name: PChar): PChar; cdecl; external 'example.so';
function repeat_string(s: PChar; n: cint64): PChar; cdecl; external 'example.so';
procedure shout_it(s: PChar); cdecl; external 'example.so';
function maybe(): cint; cdecl; external 'example.so';
function sort_json_array(s: PChar): PChar; cdecl; external 'example.so';

var
  n: Integer;
  m: cint;
begin
  for n := 1 to 10 do
    WriteLn(n, '! = ', factorial(n));

  WriteLn(greet('World'));
  WriteLn(repeat_string('ha', 3));

  shout_it('hello from yamlscript');

  m := maybe();
  if m <> 0 then
    WriteLn('maybe: true')
  else
    WriteLn('maybe: false');

  WriteLn('sorted: ', sort_json_array('[3,1,4,1,5,9,2,6]'));
end.

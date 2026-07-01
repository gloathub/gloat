with Interfaces.C;
with Interfaces.C.Strings;
with Ada.Text_IO;
with Ada.Strings;
with Ada.Strings.Fixed;

procedure Ffi_Example is
   use Interfaces.C;
   use Interfaces.C.Strings;
   use Ada.Strings;
   use Ada.Strings.Fixed;

   function Factorial (N : long) return long
     with Import, Convention => C, External_Name => "factorial";

   function Greet (Name : chars_ptr) return chars_ptr
     with Import, Convention => C, External_Name => "greet";

   function Repeat_String (S : chars_ptr; N : long) return chars_ptr
     with Import, Convention => C, External_Name => "repeat_string";

   procedure Shout_It (S : chars_ptr)
     with Import, Convention => C, External_Name => "shout_it";

   function Maybe return int
     with Import, Convention => C, External_Name => "maybe";

   function Sort_Json_Array (S : chars_ptr) return chars_ptr
     with Import, Convention => C, External_Name => "sort_json_array";

begin
   for N in 1 .. 10 loop
      Ada.Text_IO.Put_Line
        (Trim (long'Image (long (N)), Left) & "! = " &
         Trim (long'Image (Factorial (long (N))), Left));
   end loop;

   Ada.Text_IO.Put_Line (Value (Greet (New_String ("World"))));
   Ada.Text_IO.Put_Line (Value (Repeat_String (New_String ("ha"), 3)));

   Shout_It (New_String ("hello from yamlscript"));

   if Maybe /= 0 then
      Ada.Text_IO.Put_Line ("maybe: true");
   else
      Ada.Text_IO.Put_Line ("maybe: false");
   end if;

   Ada.Text_IO.Put_Line
     ("sorted: " &
      Value (Sort_Json_Array (New_String ("[3,1,4,1,5,9,2,6]"))));
end Ffi_Example;

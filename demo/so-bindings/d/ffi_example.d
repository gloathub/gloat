import core.stdc.string : strlen;
import std.conv : to;
import std.format : format;
import std.stdio : printf, writeln;
import std.string : fromStringz;

// Declare the C functions exported by example.so
extern (C)
{
    long factorial(long n);
    const(char)* greet(const(char)* name);
    const(char)* repeat_string(const(char)* s, long n);
    void shout_it(const(char)* s);
    int maybe();
    const(char)* sort_json_array(const(char)* json);
}

void main()
{
    foreach (n; 1 .. 11)
        writeln(format!"%d! = %d"(n, factorial(n)));

    writeln(greet("World").fromStringz);
    writeln(repeat_string("ha", 3).fromStringz);

    shout_it("hello from yamlscript");

    writeln(format!"maybe: %s"(maybe() != 0));

    writeln(format!"sorted: %s"(
        sort_json_array("[3,1,4,1,5,9,2,6]").fromStringz));
}

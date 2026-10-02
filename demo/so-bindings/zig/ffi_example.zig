const c = @cImport({
    @cInclude("example.h");
    @cInclude("stdio.h");
});

fn cstr(s: [*:0]const u8) [*c]u8 {
    return @ptrCast(@constCast(s));
}

pub fn main() void {
    var n: c_longlong = 1;
    while (n <= 10) : (n += 1) {
        _ = c.printf("%lld! = %lld\n", n, c.factorial(n));
    }

    _ = c.printf("%s\n", c.greet(cstr("World")));
    _ = c.printf("%s\n", c.repeat_string(cstr("ha"), 3));

    c.shout_it(cstr("hello from yamlscript"));

    const m = c.maybe();
    _ = c.printf("maybe: %s\n", if (m != 0) cstr("true") else cstr("false"));

    _ = c.printf("sorted: %s\n", c.sort_json_array(cstr("[3,1,4,1,5,9,2,6]")));
}

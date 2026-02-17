import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class FfiExample {
    public static void main(String[] args) throws Throwable {
        var linker = Linker.nativeLinker();
        var lookup = SymbolLookup.libraryLookup(
            System.getenv("LD_LIBRARY_PATH") + "/example.so",
            Arena.global());

        var factorial = linker.downcallHandle(
            lookup.find("factorial").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));

        var greet = linker.downcallHandle(
            lookup.find("greet").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        var repeatString = linker.downcallHandle(
            lookup.find("repeat_string").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

        var shoutIt = linker.downcallHandle(
            lookup.find("shout_it").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        var maybe = linker.downcallHandle(
            lookup.find("maybe").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT));

        var sortJsonArray = linker.downcallHandle(
            lookup.find("sort_json_array").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        try (var arena = Arena.ofConfined()) {
            for (long n = 1; n <= 10; n++) {
                long result = (long) factorial.invoke(n);
                System.out.printf("%d! = %d%n", n, result);
            }

            var worldPtr = arena.allocateFrom("World");
            var greetResult = (MemorySegment) greet.invoke(worldPtr);
            System.out.println(
                greetResult.reinterpret(Long.MAX_VALUE).getString(0));

            var haPtr = arena.allocateFrom("ha");
            var repeatResult =
                (MemorySegment) repeatString.invoke(haPtr, 3L);
            System.out.println(
                repeatResult.reinterpret(Long.MAX_VALUE).getString(0));

            var msgPtr = arena.allocateFrom("hello from yamlscript");
            shoutIt.invoke(msgPtr);

            int m = (int) maybe.invoke();
            System.out.println("maybe: " + (m != 0 ? "true" : "false"));

            var jsonPtr = arena.allocateFrom("[3,1,4,1,5,9,2,6]");
            var sortResult =
                (MemorySegment) sortJsonArray.invoke(jsonPtr);
            System.out.println("sorted: " +
                sortResult.reinterpret(Long.MAX_VALUE).getString(0));
        }
    }
}

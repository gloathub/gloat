#include <stdio.h>
#include <stdbool.h>
#include "example.h"

int main() {
    for (long long n = 1; n <= 10; n++)
        printf("%lld! = %lld\n", n, factorial(n));

    printf("%s\n", greet("World"));
    printf("%s\n", repeat_string("ha", 3));

    shout_it("hello from yamlscript");

    printf("maybe: %s\n", maybe() ? "true" : "false");

    printf("sorted: %s\n", sort_json_array("[3,1,4,1,5,9,2,6]"));

    return 0;
}

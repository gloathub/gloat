#include <iostream>

extern "C" {
#include "example.h"
}

int main() {
    for (long long n = 1; n <= 10; n++)
        std::cout << n << "! = " << factorial(n) << std::endl;

    std::cout << greet(const_cast<char*>("World")) << std::endl;
    std::cout << repeat_string(const_cast<char*>("ha"), 3) << std::endl;

    shout_it(const_cast<char*>("hello from yamlscript"));

    std::cout << "maybe: " << (maybe() ? "true" : "false") << std::endl;
    std::cout << "sorted: " << sort_json_array(const_cast<char*>("[3,1,4,1,5,9,2,6]")) << std::endl;

    return 0;
}

Examples
========

Gloat includes over **50 demo programs** in both Clojure and YAMLScript.
These examples demonstrate the compilation pipeline and showcase what you can
build.

All examples are available in the [`demo/`](
https://github.com/gloathub/gloat/tree/main/demo) directory of the
repository.


## Running Examples

```bash
# Run a YAMLScript example
make run FILE=demo/yamlscript/fizzbuzz.ys

# Run a Clojure example with arguments
make run FILE=demo/clojure/factorial.clj a='10'

# Compile an example to binary
gloat demo/yamlscript/dragon-curve.ys -o dragon
./dragon
```


## Try Them

The best way to explore these examples is through the [interactive demo](
demo.md):

1. Click the **Try Live Demo** badge
2. Wait for Codespaces to initialize (1-2 minutes)
3. Select any example from the dropdown
4. Click **Compile** to build to Wasm
5. Click **Run** to execute in your browser

Or run them locally:

```bash
# Clone the repository
git clone https://github.com/gloathub/gloat
cd gloat
source .rc

# Run the demo server
make demo-server

# Or run individual examples
make run FILE=demo/yamlscript/dragon-curve.ys
```


## Shared Library Bindings

Gloat can compile YAMLScript to shared libraries (`.so`/`.dylib`/`.dll`)
with auto-generated C header files, enabling FFI (Foreign Function Interface)
bindings from **23 programming languages**.

The [`demo/so-bindings/`](
https://github.com/gloathub/gloat/tree/main/demo/so-bindings) directory
contains working examples for every supported language, all calling the same
shared library compiled from a single YAMLScript source file.

### Exported Functions

The source file [`demo/so-bindings/example.ys`](
https://github.com/gloathub/gloat/tree/main/demo/so-bindings/example.ys)
defines 6 exported functions:

| Function | Signature |
|----------|-----------|
| `factorial` | `int → int` |
| `greet` | `str → str` |
| `repeat_string` | `str, int → str` |
| `shout_it` | `str → void` |
| `maybe` | `→ bool` |
| `sort_json_array` | `str → str` |

### Supported Languages

Ada, C, C++, C#, Crystal, D, Dart, Delphi, Fortran, Go, Haskell,
Java, Julia, Lua, Nim, Node.js, Perl, Python, Raku, Ruby, Rust, V,
Zig

### Building and Running

```bash
# Build the shared library and run all 23 language bindings
make test-so-bindings

# Run a single language binding
make -C demo/so-bindings/python run
```


## Contributing Examples

Have an interesting example to share?
Contributions are welcome!

See the [GitHub repository](https://github.com/gloathub/gloat) to submit
examples via pull request.

# Examples

Gloat includes over **50 example programs** in both Clojure and YAMLScript.
These examples demonstrate the compilation pipeline and showcase what you can
build.

All examples are available in the [`example/`](
https://github.com/gloathub/gloat/tree/main/example) directory of the
repository.

## Running Examples

```bash
# Run a YAMLScript example
make run FILE=example/yamlscript/fizzbuzz.ys

# Run a Clojure example with arguments
make run FILE=example/clojure/factorial.clj a='10'

# Compile an example to binary
gloat example/yamlscript/dragon-curve.ys -o dragon
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
make run FILE=example/yamlscript/dragon-curve.ys
```

## Contributing Examples

Have an interesting example to share?
Contributions are welcome!

See the [GitHub repository](https://github.com/gloathub/gloat) to submit
examples via pull request.

Compiling Your First Clojure Binary
====================================

You have Gloat installed.
Now let's give it a Clojure program and turn that program into native
executables with several different Clojure engines.

Along the way we'll compare their size and speed, use Gloat's pruning option,
and send the same program to a web page as browser WebAssembly.

**Time**: about 20 minutes, plus the first GraalVM download and build

**You'll need**: the `gloat` command from the
[first tutorial](introduction-and-installation.md) and a Unix-like shell


## Write FizzBuzz

Create a file named `fizzbuzz.clj`:

```clojure
(ns main.core)

(defn -main [& args]
  (let [n (if (seq args)
            (read-string (first args))
            100)]
    (doseq [x (map (fn [x] (cond (zero? (mod x 15)) "FizzBuzz"
                                 (zero? (mod x 5)) "Buzz"
                                 (zero? (mod x 3)) "Fizz"
                                 :else x))
                   (range 1 (inc n)))]
      (println x))))
```

This tutorial uses the `main.core` namespace.
A compiled program must have a `-main` function.
That is the function Gloat calls when the executable starts.

The `& args` parameter receives the command-line arguments.
This program reads the first one as the upper limit, or uses 100 when no
argument is supplied.


## Compile a Native Binary

Gloat's default engine is Glojure.
Compile the program to a native executable named `fizzbuzz-glj`:

```bash
gloat fizzbuzz.clj -o fizzbuzz-glj
```

Now run it with 16 as the upper limit:

```bash
./fizzbuzz-glj 16
```

```text
1
2
Fizz
4
Buzz
Fizz
7
8
Fizz
Buzz
11
Fizz
13
14
FizzBuzz
16
```

`fizzbuzz-glj` is a self-contained native executable.
It does not need Gloat, Go, Clojure, Java, or a JVM installed on the machine
where it runs.


## Make It Smaller with `-Xprune`

The normal executable contains a broad Clojure runtime.
FizzBuzz only uses a small part of that runtime, so Gloat can remove unused
parts while it builds:

```bash
gloat fizzbuzz.clj -Xprune -o fizzbuzz-glj-prune
```

Run the pruned executable to confirm that its behavior is unchanged:

```bash
./fizzbuzz-glj-prune 16
```

It prints the same output as `fizzbuzz-glj`, but the executable is much
smaller.
`-Xprune` is primarily a size optimization; always measure before drawing
conclusions about runtime speed.


## Try the let-go Engines

Gloat offers two executable-producing engines based on
[let-go](https://github.com/nooga/let-go).
The optional let-go tools are installed into Gloat's local cache the first time
one of these commands needs them.

The `lgvm` engine bundles the program for let-go's bytecode VM:

```bash
gloat fizzbuzz.clj -Elgvm -o fizzbuzz-lgvm
./fizzbuzz-lgvm 16
```

The `lglvm` engine lowers functions to native Go where it can and falls back
to the VM for the rest:

```bash
gloat fizzbuzz.clj -Elglvm -o fizzbuzz-lglvm
./fizzbuzz-lglvm 16
```

Both executables should print the same FizzBuzz output as the Glojure builds.


## Try the GraalVM Engine

Gloat can also drive GraalVM Native Image:

```bash
gloat fizzbuzz.clj -Egraalvm -o fizzbuzz-graalvm
./fizzbuzz-graalvm 16
```

The first build installs GraalVM and Leiningen into Gloat's local cache, so it
will take longer than later builds.

Gloat's GraalVM engine currently produces host-native Clojure executables
only.
It does not support cross-compilation, WebAssembly, or Gloat processing
extensions such as `-Xprune`.


## Compare Size and Speed

Get the exact size of each executable in bytes:

```bash
wc -c \
  fizzbuzz-glj \
  fizzbuzz-glj-prune \
  fizzbuzz-lgvm \
  fizzbuzz-lglvm \
  fizzbuzz-graalvm
```

For a simple runtime comparison, give each executable one warm-up run and then
time it five times.
Redirecting standard output keeps terminal rendering from dominating the
measurement:

```bash
for binary in \
  fizzbuzz-glj \
  fizzbuzz-glj-prune \
  fizzbuzz-lgvm \
  fizzbuzz-lglvm \
  fizzbuzz-graalvm
do
  echo "== $binary =="
  "./$binary" > /dev/null
  for run in 1 2 3 4 5; do
    time "./$binary" > /dev/null
  done
done
```

Use the middle `real` result after sorting the five runs.
On a Linux x86-64 laptop with an Intel Core i7-1365U, Gloat 0.1.67,
Glojure 0.7.3, let-go 1.12.2, and GraalVM 25, the results were:

| Build                  |     Size | Median runtime |
|------------------------|---------:|---------------:|
| Glojure                | 18.9 MiB |         0.007s |
| Glojure with `-Xprune` |  7.7 MiB |         0.004s |
| let-go `lgvm`          | 13.6 MiB |         0.005s |
| let-go `lglvm`         | 13.2 MiB |         0.005s |
| GraalVM Native Image   | 27.2 MiB |         0.006s |

These are sample results, not performance promises.
They measure this FizzBuzz implementation on one machine.
Run the commands on your own system when the numbers matter.

The useful result here is not that one engine always wins.
It is that Gloat gives you one interface for building and comparing several
ways to run the same Clojure source.


## Put FizzBuzz in a Web Page

The default Glojure engine can also compile the program for JavaScript-hosted
WebAssembly.
This command prunes the runtime, creates `fizzbuzz.js` and `fizzbuzz.html`,
passes 16 to `-main`, starts a local web server, and opens the page:

```bash
gloat fizzbuzz.clj -o fizzbuzz.js -Xprune,html=16,open
```

The browser page displays the same FizzBuzz output through 16.
Press **Ctrl-C** in the terminal when you are finished with the local server.

Despite its `.js` filename, `fizzbuzz.js` is the WebAssembly binary.
The generated `fizzbuzz.html` contains the Go WebAssembly JavaScript runtime,
loads that binary, passes the arguments, and captures its output for the page.


## What You Just Did

Starting with one small Clojure file, you produced:

* A native Glojure executable
* A much smaller pruned Glojure executable
* Two native let-go executables
* A GraalVM Native Image executable
* A browser-ready WebAssembly program and HTML page

See [Getting Started](../doc/getting-started.md) for the complete output-format
and engine reference, or return to the [tutorial index](index.md) for the next
walkthrough.

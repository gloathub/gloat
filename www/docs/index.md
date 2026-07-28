<div class="hero">
  <div class="hero-icon">
    <img src="img/gloat.jpeg" alt="GLOAT the Goat" class="mascot-img">
    <a href="blog/2026/02/22/introducing-gloat-and-glojure/"
       class="mascot-caption">Read all about me!</a>
  </div>
  <p class="hero-subtitle">
    Gloat (cross-)compiles Clojure code to Go code, native binaries and shared
    libraries for 25 platforms including WebAssembly
  </p>
</div>


## The Compilation Pipeline

<div class="pipeline">
  <code>foo.clj</code> <span class="arrow">→</span>
  <code>foo.go</code> <span class="arrow">→</span>
  <code>foo / foo.so / foo.wasm</code>
</div>

Gloat takes your **Clojure** source code and compiles it through multiple stages
to produce standalone executables, WebAssembly modules, or shared libraries;
for 20+ OS/Architecture combinations.

Each intermediate format can be output for inspection or further processing.


## Key Features

<div class="features">
  <div class="feature-card">
    <span class="feature-icon">🚀</span>
    <h3 class="feature-title">Zero Dependencies</h3>
    <p class="feature-desc">
      All tools (Go, Glojure, Babashka, etc) auto-install on first use.
      No manual setup required.
    </p>
  </div>

  <div class="feature-card">
    <span class="feature-icon">🌍</span>
    <h3 class="feature-title">Cross-Compile</h3>
    <p class="feature-desc">
      Build for Linux, macOS, Windows, FreeBSD, and more.
      Multiple architectures: amd64, arm64, 386.
    </p>
  </div>

  <div class="feature-card">
    <span class="feature-icon">🕸️</span>
    <h3 class="feature-title">WebAssembly</h3>
    <p class="feature-desc">
      Compile to Wasm for browser or WASI environments.
      Run your code anywhere.
    </p>
  </div>

  <div class="feature-card">
    <span class="feature-icon">📚</span>
    <h3 class="feature-title">Shared Libraries</h3>
    <p class="feature-desc">
      Create .so/.dylib/.dll files with C headers.
      Integrate with nearly any programming language via FFI using the
      Glojure or let-go engines.
    </p>
  </div>

  <div class="feature-card">
    <span class="feature-icon">🔍</span>
    <h3 class="feature-title">Transparent Pipeline</h3>
    <p class="feature-desc">
      Output any intermediate format (Clojure, Glojure, Go).
      Understand and debug every step.
    </p>
  </div>

  <div class="feature-card">
    <span class="feature-icon">📦</span>
    <h3 class="feature-title">Portable Builds</h3>
    <p class="feature-desc">
      Generate standalone Go project directories.
      Build anywhere with just Make.
    </p>
  </div>
</div>


## Quick Example

```bash
# Compile to native binary
gloat hello.clj

# Cross-compile for Windows
gloat app.clj -o app.exe -p windows/amd64

# Create WebAssembly module
gloat program.clj -o program.wasm

# Output intermediate formats
gloat code.clj -t glj  # See generated Glojure
gloat code.clj -t go   # See generated Go

# Format and syntax highlight Clojure
gloat -F code.clj              # Format with zprint
gloat -C code.clj | less -R    # Syntax highlight with ANSI colors
gloat -FCw40 code.clj | less -R # Format at width 40, then highlight

# Source input defaults to stdin when omitted
cat code.clj | gloat -FC | less -R

# Create a Go build directory
gloat code.clj -o code/
make -C code/ build     # Compile to binary
```


## Get Started

Ready to compile your Clojure code?

<div class="hero-cta">
  <a href="demo/" class="cta-button cta-primary">Try the Demo</a>
  <a href="doc/getting-started/" class="cta-button cta-secondary">Installation Guide</a>
  <a href="examples/" class="cta-button cta-secondary">See Examples</a>
</div>

<div class="footer-credit">
  Created by <a href="https://github.com/ingydotnet">Ingy döt Net</a>
</div>

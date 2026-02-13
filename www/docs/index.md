<div class="hero">
  <div class="hero-icon">⚙️</div>
  <p class="hero-subtitle">
    Gloat compiles Clojure & YAMLScript to native binaries and Wasm
  </p>
</div>

## The Compilation Pipeline

<div class="pipeline">
  <code>.ys</code> <span class="arrow">→</span>
  <code>.clj</code> <span class="arrow">→</span>
  <code>.glj</code> <span class="arrow">→</span>
  <code>.go</code> <span class="arrow">→</span>
  <code>binary/wasm</code>
</div>

Gloat takes your **Clojure** or **YAMLScript** source code and compiles it
through multiple stages to produce standalone executables, WebAssembly modules,
or shared libraries.

Each intermediate format can be output for inspection or further processing.

## Key Features

<div class="features">
  <div class="feature-card">
    <span class="feature-icon">🚀</span>
    <h3 class="feature-title">Zero Dependencies</h3>
    <p class="feature-desc">
      All tools (Go, Glojure, YAMLScript) auto-install on first use.
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
      Integrate with any language via FFI.
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
gloat hello.ys

# Cross-compile for Windows
gloat app.clj -o app.exe -p windows/amd64

# Create WebAssembly module
gloat program.ys -o program.wasm

# Output intermediate formats
gloat code.ys -t clj   # See generated Clojure
gloat code.ys -t glj   # See generated Glojure
gloat code.ys -t go    # See generated Go
```

## Get Started

Ready to compile your Clojure or YAMLScript code?

<div class="hero-cta">
  <a href="demo/" class="cta-button cta-primary">Try the Demo</a>
  <a href="getting-started/" class="cta-button cta-secondary">Installation Guide</a>
  <a href="examples/" class="cta-button cta-secondary">See Examples</a>
</div>

<div class="footer-credit">
  Created by <a href="https://github.com/ingydotnet">Ingy döt Net</a>
</div>

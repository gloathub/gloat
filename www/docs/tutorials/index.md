Tutorials
=========

Hands-on walkthroughs that take you from a blank file to something running.

Each tutorial is a complete, followable session: every command is meant to be
typed, and every output shown is what you should see.
If you just want the reference material instead, see the
[Documentation](../doc/index.md) pages.


## Start Here

Work through these in order.

<div class="features">
  <a href="introduction-and-installation/" class="feature-card tutorial-card">
    <span class="feature-icon">🐐</span>
    <h3 class="feature-title">1. Introduction and Installation</h3>
    <p class="feature-desc">
      What Gloat is, what it can produce, and how to get a working
      <code>gloat</code> command - including where it puts everything and how
      to keep it current.
    </p>
  </a>

  <a href="compiling-your-first-clojure-binary/"
     class="feature-card tutorial-card">
    <span class="feature-icon">🚀</span>
    <h3 class="feature-title">2. Compiling Your First Clojure Binary</h3>
    <p class="feature-desc">
      Write a Clojure program and take it all the way to a self-contained
      native executable - then to another platform, and into the browser.
    </p>
  </a>
</div>


## Coming Soon

More tutorials are planned.
If one of these is what you need next, say so in an
[issue](https://github.com/gloathub/gloat/issues) and it will get written
sooner.

<div class="features">
  <div class="feature-card tutorial-card tutorial-card--planned">
    <span class="feature-icon">🔌</span>
    <h3 class="feature-title">Calling Go Libraries</h3>
    <p class="feature-desc">
      Pull a Go module into your program with <code>gljdeps.edn</code> and call
      it from Glojure.
    </p>
  </div>

  <div class="feature-card tutorial-card tutorial-card--planned">
    <span class="feature-icon">📚</span>
    <h3 class="feature-title">Building a Shared Library</h3>
    <p class="feature-desc">
      Compile to a <code>.so</code> with a generated C header, then call it
      from Python, Ruby, and Rust.
    </p>
  </div>

  <div class="feature-card tutorial-card tutorial-card--planned">
    <span class="feature-icon">🔁</span>
    <h3 class="feature-title">REPL-Driven Development</h3>
    <p class="feature-desc">
      Use the Glojure REPL, nREPL, and socket REPL to develop against a live
      program.
    </p>
  </div>

  <div class="feature-card tutorial-card tutorial-card--planned">
    <span class="feature-icon">🚢</span>
    <h3 class="feature-title">Shipping a CLI Tool</h3>
    <p class="feature-desc">
      Turn a script into a released project with Makes, multi-platform
      binaries, and <code>go install</code> support.
    </p>
  </div>
</div>


## Other Learning Material

- [Getting Started](../doc/getting-started.md) - the condensed tour of every
  Gloat feature.
- [Demo](../demo.md) - compile and run programs in your browser, nothing to
  install.
- [Examples](../examples.md) - 50+ working programs in Clojure and YAMLScript.
- [Ecosystem Tutorial](../doc/gloat-tutorial.md) - how the gloat, glojure,
  makes, and downstream repositories fit together.

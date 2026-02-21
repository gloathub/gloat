---
title: Live Demo
hide:
- navigation
- toc
---

<link rel="stylesheet"
  href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css"
  media="(prefers-color-scheme: light)">
<link rel="stylesheet"
  href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css"
  media="(prefers-color-scheme: dark)">
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/yaml.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/clojure.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/go.min.js"></script>
<script src="../demo-assets/wasm_exec.js"></script>
<script src="../javascripts/demo.js"></script>

<p id="demo-intro">Run pre-compiled Wasm programs.
Select a program, view the Source/Glojure/Go, click <strong>Run</strong>.</p>

<!-- Program selector + language toggle -->
<div class="demo-selector">
  <div class="demo-lang-toggle">
    <input type="radio" name="demo-lang" id="lang-ys"
           value="yamlscript" checked>
    <label for="lang-ys">YAMLScript</label>
    <input type="radio" name="demo-lang" id="lang-clj"
           value="clojure">
    <label for="lang-clj">Clojure</label>
  </div>
  <select id="demo-program"></select>
</div>

<div class="demo-layout">

<!-- Left column: source + intermediates -->
<div class="demo-left">

<!-- Source code panel -->
<div class="demo-code-panel demo-collapsible" id="demo-source">
  <div class="demo-panel-header" onclick="demoAccordion(this)">
    <span class="demo-source-title">YAMLScript Source</span>
    <span class="demo-toggle-icon"></span>
  </div>
  <div class="demo-pre-wrap">
    <button class="demo-copy-btn" onclick="demoCopy(this)" title="Copy">&#x2398;</button>
    <pre><code></code></pre>
  </div>
</div>

<!-- Glojure intermediate -->
<div class="demo-code-panel demo-collapsible demo-collapsed" id="demo-glj">
  <div class="demo-panel-header" onclick="demoAccordion(this)">
    <span>Glojure (intermediate)</span>
    <span class="demo-toggle-icon"></span>
  </div>
  <div class="demo-pre-wrap">
    <button class="demo-copy-btn" onclick="demoCopy(this)" title="Copy">&#x2398;</button>
    <pre><code></code></pre>
  </div>
</div>

<!-- Go intermediate -->
<div class="demo-code-panel demo-collapsible demo-collapsed" id="demo-go">
  <div class="demo-panel-header" onclick="demoAccordion(this)">
    <span>Go (intermediate)</span>
    <span class="demo-toggle-icon"></span>
  </div>
  <div class="demo-pre-wrap">
    <button class="demo-copy-btn" onclick="demoCopy(this)" title="Copy">&#x2398;</button>
    <pre><code></code></pre>
  </div>
</div>

</div>

<!-- Right column: controls + output -->
<div class="demo-right">

<div class="demo-controls">
  <button id="demo-run" class="demo-run-btn">Run</button>
  <select id="demo-args"></select>
</div>

<div id="demo-loading" class="demo-loading"></div>

<div class="demo-output-panel" id="demo-output">
  <div class="demo-panel-header">
    <span>Output</span>
  </div>
  <pre class="demo-output-text"></pre>
</div>

</div>

</div>

---

## Want more programs?

This static demo includes 4 programs pre-compiled to Glojure, Go and Wasm.

For the full 25+ program experience with live editing and compilation, try the
[Codespaces demo](https://codespaces.new/gloathub/gloat?quickstart=1).

Or run it locally:

```bash
git clone https://github.com/gloathub/gloat
cd gloat
make serve-demo
```

> **Note** Running locally is much faster, as **it takes the Codespaces demo up
> to 2 minutes to fully warm up**.
>
> On the other hand, the Codespaces demo allows you to do everything without
> running any code locally.

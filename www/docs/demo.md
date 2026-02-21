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
  <a href=".." class="demo-home-link" title="Home">&#x25C0;</a>
  <div class="demo-lang-toggle">
    <input type="radio" name="demo-lang" id="lang-clj"
           value="clojure" checked>
    <label for="lang-clj">Clojure</label>
    <input type="radio" name="demo-lang" id="lang-ys"
           value="yamlscript">
    <label for="lang-ys">YAMLScript</label>
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
    <span><button class="demo-raw-link demo-view-btn" onclick="demoCopy(this)" title="Copy"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg></button><a id="demo-source-link" class="demo-raw-link" href="#" download onclick="event.stopPropagation()" title="Download"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M5 20h14v-2H5v2zm7-18v10.17l-3.59-3.58L7 10l5 5 5-5-1.41-1.41L13 12.17V2h-2z"/></svg></a><button class="demo-raw-link demo-view-btn" onclick="demoViewCode(event, 'demo-source')" title="Open in new tab"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 19H5V5h7V3H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z"/></svg></button><span class="demo-toggle-icon"></span></span>
  </div>
  <div class="demo-pre-wrap">
    <pre><code></code></pre>
  </div>
</div>

<!-- Glojure intermediate -->
<div class="demo-code-panel demo-collapsible demo-collapsed" id="demo-glj">
  <div class="demo-panel-header" onclick="demoAccordion(this)">
    <span>Glojure (intermediate)</span>
    <span><button class="demo-raw-link demo-view-btn" onclick="demoCopy(this)" title="Copy"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg></button><a id="demo-glj-link" class="demo-raw-link" href="#" download onclick="event.stopPropagation()" title="Download"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M5 20h14v-2H5v2zm7-18v10.17l-3.59-3.58L7 10l5 5 5-5-1.41-1.41L13 12.17V2h-2z"/></svg></a><button class="demo-raw-link demo-view-btn" onclick="demoViewCode(event, 'demo-glj')" title="Open in new tab"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 19H5V5h7V3H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z"/></svg></button><span class="demo-toggle-icon"></span></span>
  </div>
  <div class="demo-pre-wrap">
    <pre><code></code></pre>
  </div>
</div>

<!-- Go intermediate -->
<div class="demo-code-panel demo-collapsible demo-collapsed" id="demo-go">
  <div class="demo-panel-header" onclick="demoAccordion(this)">
    <span>Go (intermediate)</span>
    <span><button class="demo-raw-link demo-view-btn" onclick="demoCopy(this)" title="Copy"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg></button><a id="demo-go-link" class="demo-raw-link" href="#" download onclick="event.stopPropagation()" title="Download"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M5 20h14v-2H5v2zm7-18v10.17l-3.59-3.58L7 10l5 5 5-5-1.41-1.41L13 12.17V2h-2z"/></svg></a><button class="demo-raw-link demo-view-btn" onclick="demoViewCode(event, 'demo-go')" title="Open in new tab"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 19H5V5h7V3H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z"/></svg></button><span class="demo-toggle-icon"></span></span>
  </div>
  <div class="demo-pre-wrap">
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
    <span onclick="demoResetCheck(event)">Output</span>
    <span class="demo-output-scroll-btns" id="demo-output-scroll-btns" style="display:none">
      <button class="demo-raw-link demo-view-btn demo-scroll-btn" onclick="demoScrollOutput('top')" title="Scroll to top">&#x25B3;</button>
      <button class="demo-raw-link demo-view-btn demo-scroll-btn" onclick="demoScrollOutput('bottom')" title="Scroll to bottom">&#x25BD;</button>
    </span>
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

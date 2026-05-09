---
title: Glojure REPL
hide:
- navigation
- toc
---

<style>
/* Hide tabs on REPL page */
body:has(#repl-container) .md-tabs {
  display: none;
}

/* REPL toolbar */
#repl-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #1a1a2e;
  border: 2px solid var(--gl-code-border);
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  padding: 0.25rem 0.5rem;
}

#repl-toolbar:has(+ #repl-container:focus-within) {
  border-color: var(--gl-green);
  box-shadow: 0 -2px 8px var(--gl-glow);
}

/* Inline toolbar buttons */
.repl-buttons {
  display: flex;
  gap: 0.5rem;
}

.repl-btn {
  background: transparent;
  border: 1px solid #444;
  border-radius: 4px;
  color: #aaa;
  font-size: 0.85rem;
  padding: 0.15rem 0.35rem;
  cursor: pointer;
  line-height: 1;
}

.repl-btn:hover {
  color: #e0e0e0;
  border-color: #666;
}

/* Dropdown wrapper */
.repl-menu {
  position: relative;
}

.repl-menu-btn {
  background: transparent;
  border: 1px solid #444;
  border-radius: 4px;
  color: #aaa;
  font-size: 1rem;
  padding: 0.15rem 0.4rem;
  cursor: pointer;
  line-height: 1;
}

.repl-menu-btn:hover {
  color: #e0e0e0;
  border-color: #666;
}

.repl-menu-items {
  display: none;
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 2px;
  background: #252545;
  border: 1px solid #444;
  border-radius: 6px;
  min-width: 160px;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4);
  padding: 0.25rem 0;
}

.repl-menu.open .repl-menu-items {
  display: block;
}

.repl-menu-items button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  background: transparent;
  border: none;
  color: #ccc;
  font-family: 'Roboto Mono', monospace;
  font-size: 0.8rem;
  padding: 0.4rem 0.75rem;
  cursor: pointer;
  text-align: left;
}

.repl-menu-items button:hover {
  background: #333360;
  color: #e0e0e0;
}

.repl-menu-items .menu-icon {
  width: 1.2em;
  text-align: center;
  flex-shrink: 0;
}

.repl-menu-items .menu-key {
  margin-left: auto;
  color: #666;
  font-size: 0.7rem;
}

/* REPL terminal */
#repl-container {
  background: #1a1a2e;
  color: #e0e0e0;
  border: 2px solid var(--gl-code-border);
  border-radius: 0 0 8px 8px;
  padding: 1rem;
  font-family: 'Roboto Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  height: calc(100vh - 210px);
  min-height: 370px;
  overflow-y: auto;
  cursor: text;
}

#repl-container:focus-within {
  border-color: var(--gl-green);
  box-shadow: 0 0 8px var(--gl-glow);
}

#repl-output span {
  white-space: pre-wrap;
  word-wrap: break-word;
}

#repl-input {
  display: inline-block;
  vertical-align: top;
  background: transparent;
  color: #e0e0e0;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  border: none;
  outline: none;
  caret-color: var(--gl-accent);
  min-width: 1ch;
}

#repl-input:empty::before {
  content: '';
}

/* Loading overlay */
#repl-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 180px);
  min-height: 400px;
  background: #1a1a2e;
  border: 2px solid var(--gl-code-border);
  border-radius: 8px;
  color: var(--gl-green-light);
  font-family: 'Roboto Mono', monospace;
  font-size: 1rem;
}

#repl-loading .spinner {
  display: inline-block;
  width: 1.2em;
  height: 1.2em;
  border: 2px solid var(--gl-green-light);
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 0.75rem;
  vertical-align: middle;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Error styling */
.repl-error {
  color: #ff6b6b;
}

/* Prompt styling */
.repl-prompt {
  color: var(--gl-accent);
}

/* Syntax highlighting - matches CLI ANSI colors */
.hl-string  { color: #22c55e; }  /* green - \x1b[32m */
.hl-keyword { color: #06b6d4; }  /* cyan  - \x1b[36m */
.hl-literal { color: #c084fc; }  /* magenta - \x1b[35m */
.hl-special { color: #facc15; font-weight: bold; }  /* bold yellow - \x1b[1;33m */
.hl-comment { color: #6b7280; }  /* gray - \x1b[90m */
.hl-core    { color: #5b8dd9; }  /* blue - \x1b[38;5;69m */
.hl-symbol  { color: #e0e0e0; }  /* default text */
</style>

<div id="repl-loading">
  <span><span class="spinner"></span>Loading Glojure REPL...</span>
</div>

<div id="repl-toolbar" style="display:none">
  <div class="repl-buttons">
    <button class="repl-btn" data-action="share" title="Copy share URL">&#x21D7;</button>
    <button class="repl-btn" data-action="copy-form" title="Copy current form">&#x29C9;</button>
    <button class="repl-btn" data-action="history-prev" title="History prev (Up)">&uarr;</button>
    <button class="repl-btn" data-action="history-next" title="History next (Down)">&darr;</button>
    <button class="repl-btn" data-action="kill-before" title="Kill before cursor (^U)">&lArr;</button>
    <button class="repl-btn" data-action="kill-after" title="Kill after cursor (^K)">&rArr;</button>
    <button class="repl-btn" data-action="clear" title="Clear screen (^L)">&#x2715;</button>
  </div>
  <!--
  <div class="repl-menu" id="repl-ctrl-menu">
    <button class="repl-menu-btn" title="More controls">&#9776;</button>
    <div class="repl-menu-items">
      <button data-action="home">
        <span class="menu-icon">&#x21E4;</span> Line start
        <span class="menu-key">^A</span>
      </button>
      <button data-action="end">
        <span class="menu-icon">&#x21E5;</span> Line end
        <span class="menu-key">^E</span>
      </button>
      <button data-action="kill-before">
        <span class="menu-icon">&#x232B;</span> Kill before
        <span class="menu-key">^U</span>
      </button>
      <button data-action="kill-after">
        <span class="menu-icon">&#x2326;</span> Kill after
        <span class="menu-key">^K</span>
      </button>
    </div>
  </div>
  -->
</div>

<div id="repl-container" style="display:none"
     onclick="if (!window.getSelection().toString()) document.getElementById('repl-input').focus()">
  <div id="repl-output">
    <span id="repl-input" contenteditable="true"
          autocomplete="off" spellcheck="false"></span>
  </div>
</div>


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

/* REPL terminal */
#repl-container {
  background: #1a1a2e;
  color: #e0e0e0;
  border: 2px solid var(--gl-code-border);
  border-radius: 8px;
  padding: 1rem;
  font-family: 'Roboto Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  height: calc(100vh - 180px);
  min-height: 400px;
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
</style>

<div id="repl-loading">
  <span><span class="spinner"></span>Loading Glojure REPL...</span>
</div>

<div id="repl-container" style="display:none"
     onclick="document.getElementById('repl-input').focus()">
  <div id="repl-output">
    <span id="repl-input" contenteditable="true"
          autocomplete="off" spellcheck="false"></span>
  </div>
</div>


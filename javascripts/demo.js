// Static WASM Demo for GloatHub
// Loads pre-compiled WASM programs and runs them in the browser

// Copy code to clipboard
function demoCopy(btn) {
  event.stopPropagation();
  var panel = btn.closest('.demo-code-panel');
  if (!panel) return;
  var code = panel.querySelector('code');
  if (!code) return;
  navigator.clipboard.writeText(code.textContent).then(function() {
    btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>';
    setTimeout(function() { btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg>'; }, 1500);
  });
}

// Easter egg: triple-click the home arrow to clear demo localStorage
function demoResetCheck(e) {
  if (e.detail === 3) {
    e.preventDefault();
    Object.keys(localStorage).forEach(function(k) {
      if (k.startsWith('demo-')) localStorage.removeItem(k);
    });
    location.reload();
  }
}

// Open code panel content in a new tab as plain text
function demoViewCode(e, panelId) {
  e.stopPropagation();
  var code = document.getElementById(panelId).querySelector('code');
  if (!code) return;
  var blob = new Blob([code.textContent], { type: 'text/plain' });
  var url = URL.createObjectURL(blob);
  window.open(url, '_blank');
}

// Scroll output pane to top or bottom
function demoScrollOutput(dir) {
  var el = document.querySelector('.demo-output-text');
  if (!el) return;
  el.scrollTo({ top: dir === 'top' ? 0 : el.scrollHeight, behavior: 'smooth' });
}

// Accordion: expanding one panel collapses the other two
function demoAccordion(header) {
  var panel = header.parentElement;
  var isCollapsed = panel.classList.contains('demo-collapsed');
  if (isCollapsed) {
    // Collapse all siblings, expand this one
    var siblings = panel.parentElement.querySelectorAll('.demo-code-panel');
    siblings.forEach(function(s) { s.classList.add('demo-collapsed'); });
    panel.classList.remove('demo-collapsed');
  } else {
    panel.classList.add('demo-collapsed');
  }
}

(function() {
  'use strict';

  const ASSETS = '../demo-assets';

  let config = {};
  let cachedModules = {};  // lang/prog -> compiled WebAssembly.Module
  let currentLang = 'clojure';
  let currentProg = '';

  // DOM elements (resolved after DOMContentLoaded)
  let programSelect, langToggle, argSelect, runBtn;
  let sourcePanel, gljPanel, goPanel, outputPanel;
  let loadingIndicator;

  // Source file extensions per language
  const srcExt = { yamlscript: 'ys', clojure: 'clj' };

  // Display names per language
  const langName = { yamlscript: 'YAMLScript', clojure: 'Clojure' };

  // highlight.js language names
  const hlLang = { ys: 'yaml', clj: 'clojure', glj: 'clojure', go: 'go' };

  // Fetch a text file from demo-assets
  function fetchAsset(path) {
    return fetch(`${ASSETS}/${path}`).then(r => {
      if (!r.ok) throw new Error(`Failed to load ${path}: ${r.status}`);
      return r.text();
    });
  }

  // Highlight code and set it into a panel's <code> element
  function displayCode(panel, code, lang) {
    var pre = panel.querySelector('pre');
    if (!pre) return;
    // Replace the <code> element to clear hljs state
    var codeEl = document.createElement('code');
    codeEl.className = lang ? 'language-' + lang : '';
    codeEl.textContent = code;
    pre.innerHTML = '';
    pre.appendChild(codeEl);
    if (lang && window.hljs) {
      window.hljs.highlightElement(codeEl);
    }
  }

  // Escape HTML entities
  function escapeHtml(str) {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  // Populate the program selector from config
  function populatePrograms() {
    programSelect.innerHTML = '';
    const programs = Object.keys(config).sort();
    programs.forEach(function(name) {
      const opt = document.createElement('option');
      opt.value = name;
      opt.textContent = name;
      programSelect.appendChild(opt);
    });
    if (programs.length > 0 && !currentProg) {
      currentProg = programs[0];
      programSelect.value = currentProg;
    }
  }

  // Populate argument selector for the current program
  function populateArgs() {
    argSelect.innerHTML = '';
    const progConfig = config[currentProg];
    if (!progConfig || !Array.isArray(progConfig) ||
        progConfig.length === 0 || !Array.isArray(progConfig[0]) ||
        progConfig[0].length === 0) {
      // No args for this program
      const opt = document.createElement('option');
      opt.value = '[]';
      opt.textContent = '(no arguments)';
      argSelect.appendChild(opt);
      return;
    }

    const argList = progConfig[0];
    // Detect labeled format: [["label", value, default?], ...]
    var isLabeled = Array.isArray(argList[0]) &&
      typeof argList[0][0] === 'string';

    if (isLabeled) {
      argList.forEach(function(item) {
        var opt = document.createElement('option');
        opt.textContent = item[0];
        // Store arrays as JSON, primitives as-is
        opt.value = Array.isArray(item[1])
          ? JSON.stringify(item[1]) : String(item[1]);
        if (item[2]) opt.selected = true;
        argSelect.appendChild(opt);
      });
    } else {
      // Simple format: [val1, [defaultVal], val3, ...]
      // Bracketed value is the default
      argList.forEach(function(item) {
        var opt = document.createElement('option');
        var isDefault = Array.isArray(item);
        var value = isDefault ? item[0] : item;
        opt.textContent = String(value);
        opt.value = String(value);
        if (isDefault) opt.selected = true;
        argSelect.appendChild(opt);
      });
    }
  }

  // Load and display source + intermediates for current selection
  async function loadProgram() {
    const ext = srcExt[currentLang];
    const srcPath = `${currentLang}/src/${currentProg}.${ext}`;
    const gljPath = `${currentLang}/glj/${currentProg}.glj`;
    const goPath = `${currentLang}/go/${currentProg}.go`;

    // Update source panel title and raw links
    var titleEl = sourcePanel.querySelector('.demo-source-title');
    if (titleEl) titleEl.textContent = langName[currentLang] + ' Source';
    var srcLink = document.getElementById('demo-source-link');
    var gljLink = document.getElementById('demo-glj-link');
    var goLink = document.getElementById('demo-go-link');
    if (srcLink) srcLink.href = ASSETS + '/' + srcPath;
    if (gljLink) gljLink.href = ASSETS + '/' + gljPath;
    if (goLink) goLink.href = ASSETS + '/' + goPath;

    // Clear output
    outputPanel.querySelector('.demo-output-text').textContent = '';

    // Load all three files in parallel
    try {
      const [src, glj, go] = await Promise.all([
        fetchAsset(srcPath),
        fetchAsset(gljPath),
        fetchAsset(goPath)
      ]);

      displayCode(sourcePanel, src, hlLang[ext]);
      displayCode(gljPanel, glj, hlLang.glj);
      displayCode(goPanel, go, hlLang.go);
    } catch (err) {
      displayCode(sourcePanel, 'Error loading source: ' + err.message, '');
    }

    populateArgs();

    // Restore saved argument selection
    var savedArg = localStorage.getItem('demo-arg-' + currentProg);
    if (savedArg && argSelect.querySelector('option[value="' + savedArg + '"]')) {
      argSelect.value = savedArg;
    }
  }

  // Get the cache key for current selection
  function cacheKey() {
    return currentLang + '/' + currentProg;
  }

  // Show/hide output scroll buttons based on scrollability
  function updateScrollBtns() {
    var el = outputPanel ? outputPanel.querySelector('.demo-output-text') : null;
    var btns = document.getElementById('demo-output-scroll-btns');
    if (!el || !btns) return;
    btns.style.display = el.scrollHeight > el.clientHeight ? 'inline-flex' : 'none';
  }

  // Run the selected program
  async function runProgram() {
    runBtn.disabled = true;
    const outputText = outputPanel.querySelector('.demo-output-text');
    const key = cacheKey();

    // Parse args
    let args = [];
    const rawValue = argSelect.value;
    try {
      const parsed = JSON.parse(rawValue);
      args = Array.isArray(parsed)
        ? parsed.map(String)
        : [String(parsed)];
    } catch(e) {
      args = [rawValue];
    }

    const argsDisplay = args.length > 0 ? ' ' + args.join(' ') : '';

    // Check if we have a cached module
    if (!cachedModules[key]) {
      // Need to download the JS/WASM file
      outputText.innerHTML =
        '<span class="demo-feedback">Downloading WASM module for ' +
        escapeHtml(currentProg) + '...\n' +
        'This is a one-time ~12MB download (compressed).</span>';

      try {
        const jsPath = `${ASSETS}/${currentLang}/js/${currentProg}.js`;
        loadingIndicator.style.display = 'block';

        const response = await fetch(jsPath);
        if (!response.ok) throw new Error('HTTP ' + response.status);

        const contentLength = response.headers.get('content-length');
        const total = contentLength ? parseInt(contentLength, 10) : 0;
        let loaded = 0;

        // Stream the response to track progress
        const reader = response.body.getReader();
        const chunks = [];

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          chunks.push(value);
          loaded += value.length;
          if (total > 0) {
            const pct = Math.round(loaded / total * 100);
            const mb = (loaded / 1048576).toFixed(1);
            const totalMb = (total / 1048576).toFixed(1);
            loadingIndicator.textContent =
              'Downloading: ' + mb + ' / ' + totalMb + ' MB (' + pct + '%)';
          } else {
            const mb = (loaded / 1048576).toFixed(1);
            loadingIndicator.textContent = 'Downloading: ' + mb + ' MB...';
          }
        }

        loadingIndicator.style.display = 'none';

        // Concatenate chunks
        const totalLength = chunks.reduce(function(acc, c) {
          return acc + c.length;
        }, 0);
        const wasmBytes = new Uint8Array(totalLength);
        let offset = 0;
        chunks.forEach(function(chunk) {
          wasmBytes.set(chunk, offset);
          offset += chunk.length;
        });

        outputText.innerHTML =
          '<span class="demo-feedback">Compiling WASM module...</span>';

        // Give browser a moment to render
        await new Promise(function(r) { setTimeout(r, 10); });

        cachedModules[key] = await WebAssembly.compile(wasmBytes);

        outputText.innerHTML =
          '<span class="demo-feedback">WASM module ready. Running: ' +
          escapeHtml(currentProg) + escapeHtml(argsDisplay) + '</span>\n';

      } catch (err) {
        outputText.innerHTML =
          '<span class="demo-feedback">Error downloading WASM: ' +
          escapeHtml(String(err)) + '</span>';
        loadingIndicator.style.display = 'none';
        runBtn.disabled = false;
        return;
      }
    } else {
      outputText.innerHTML =
        '<span class="demo-feedback">Running: ' +
        escapeHtml(currentProg) + escapeHtml(argsDisplay) +
        '</span>\n';
    }

    // Give browser a moment to render before Go runtime blocks
    await new Promise(function(r) { setTimeout(r, 10); });

    // Buffer console.log output during WASM execution
    const originalLog = console.log;
    const outputBuffer = [];
    console.log = function() {
      const line = Array.prototype.slice.call(arguments).join(' ');
      outputBuffer.push(line);
    };

    try {
      const go = new Go();
      go.argv = ['program'].concat(args);

      const instance = await WebAssembly.instantiate(
        cachedModules[key], go.importObject
      );

      const runStart = performance.now();
      await go.run(instance);
      const runMs = Math.round(performance.now() - runStart);

      // Render buffered output
      const escaped = outputBuffer.map(escapeHtml).join('\n');
      outputText.innerHTML += escaped + '\n';
      outputText.innerHTML +=
        '<span class="demo-feedback">Program complete (' + runMs + 'ms).</span>';
      setTimeout(updateScrollBtns, 50);

    } catch (err) {
      outputText.innerHTML +=
        '<span class="demo-feedback">Runtime error: ' +
        escapeHtml(String(err)) + '</span>';
    } finally {
      console.log = originalLog;
      runBtn.disabled = false;
    }
  }

  // Initialize demo (called on page load and instant navigation)
  async function initDemo() {
    programSelect = document.getElementById('demo-program');
    if (!programSelect) return;  // Not on demo page

    langToggle = document.querySelectorAll('input[name="demo-lang"]');
    argSelect = document.getElementById('demo-args');
    runBtn = document.getElementById('demo-run');
    sourcePanel = document.getElementById('demo-source');
    gljPanel = document.getElementById('demo-glj');
    goPanel = document.getElementById('demo-go');
    outputPanel = document.getElementById('demo-output');
    loadingIndicator = document.getElementById('demo-loading');

    // Load config
    try {
      const resp = await fetch(ASSETS + '/config.json');
      config = await resp.json();
    } catch (err) {
      outputPanel.querySelector('.demo-output-text').textContent =
        'Error loading config: ' + err;
      return;
    }

    currentLang = localStorage.getItem('demo-lang') || 'clojure';
    currentProg = localStorage.getItem('demo-prog') || '';

    populatePrograms();

    // Restore saved program selection
    if (currentProg && programSelect.querySelector('option[value="' + currentProg + '"]')) {
      programSelect.value = currentProg;
    } else {
      currentProg = programSelect.value;
    }

    // Restore saved language selection
    langToggle.forEach(function(radio) {
      radio.checked = (radio.value === currentLang);
    });

    // Event: program change
    programSelect.addEventListener('change', function() {
      currentProg = this.value;
      localStorage.setItem('demo-prog', currentProg);
      loadProgram();
    });

    // Event: language toggle
    langToggle.forEach(function(radio) {
      radio.addEventListener('change', function() {
        currentLang = this.value;
        localStorage.setItem('demo-lang', currentLang);
        loadProgram();
      });
    });

    // Event: argument change
    argSelect.addEventListener('change', function() {
      localStorage.setItem('demo-arg-' + currentProg, this.value);
    });

    // Event: run button
    runBtn.addEventListener('click', runProgram);

    // Size panes to fill to bottom of viewport
    function fitOutputPane() {
      var el = outputPanel.querySelector('.demo-output-text');
      var layout = document.querySelector('.demo-layout');
      if (!el || !layout) return;

      // In mobile (stacked column), let CSS handle sizing
      if (window.innerWidth <= 960) {
        layout.style.height = '';
        el.style.minHeight = '200px';
        el.style.maxHeight = '';
        setTimeout(updateScrollBtns, 0);
        return;
      }

      var layoutTop = layout.getBoundingClientRect().top;
      var margin = 16;
      var totalH = Math.max(window.innerHeight - layoutTop - margin, 300);
      // Set the layout height so both columns are equal via CSS stretch
      layout.style.height = totalH + 'px';
      // Output text fills whatever remains in right column
      var rightTop = document.querySelector('.demo-right').getBoundingClientRect().top;
      var textTop = el.getBoundingClientRect().top;
      var textH = Math.max(totalH - (textTop - rightTop), 200);
      el.style.minHeight = textH + 'px';
      el.style.maxHeight = textH + 'px';
      setTimeout(updateScrollBtns, 0);
    }
    requestAnimationFrame(function() {
      fitOutputPane();
    });
    window.addEventListener('resize', fitOutputPane);

    // Load first program
    await loadProgram();

    // Fade out intro text after 5 seconds
    var intro = document.getElementById('demo-intro');
    if (intro) {
      setTimeout(function() {
        intro.style.transition = 'opacity 1s ease, height 0.5s ease 0.8s, margin 0.5s ease 0.8s, padding 0.5s ease 0.8s';
        intro.style.opacity = '0';
        intro.style.height = '0';
        intro.style.margin = '0';
        intro.style.padding = '0';
        intro.style.overflow = 'hidden';
      }, 5000);
    }
  }

  // Support both full page loads and MkDocs instant navigation
  // Use document$ if available (instant nav), otherwise DOMContentLoaded
  var initialized = false;
  async function initOnce() {
    if (initialized && document.getElementById('demo-program')) return;
    initialized = true;
    await initDemo();
  }

  if (typeof document$ !== 'undefined') {
    document$.subscribe(function() {
      initialized = false;
      initOnce();
    });
  } else {
    document.addEventListener('DOMContentLoaded', initOnce);
  }
})();

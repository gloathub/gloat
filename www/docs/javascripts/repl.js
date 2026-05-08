(function() {
  var wasmLoaded = false;
  var replRunning = false;

  function loadScript(src) {
    return new Promise(function(resolve, reject) {
      var script = document.createElement('script');
      script.src = src;
      script.onload = resolve;
      script.onerror = reject;
      document.head.appendChild(script);
    });
  }

  function initRepl() {
    var loading = document.getElementById('repl-loading');
    if (!loading || replRunning) return;
    replRunning = true;

    // Polyfill for older browsers
    if (!WebAssembly.instantiateStreaming) {
      WebAssembly.instantiateStreaming = async function(resp, importObject) {
        var source = await (await resp).arrayBuffer();
        return await WebAssembly.instantiate(source, importObject);
      };
    }

    var WASM_URL = '/repl/glj.wasm';

    var p = wasmLoaded
      ? Promise.resolve()
      : loadScript('/repl/wasm_exec.js').then(function() {
          wasmLoaded = true;
        });

    p.then(function() {
      var go = new Go();
      return WebAssembly.instantiateStreaming(
        fetch(WASM_URL),
        go.importObject
      ).then(function(result) {
        run(go, result.module, result.instance);
      });
    }).catch(function(err) {
      console.error('WASM load error:', err);
      loading.innerHTML = '<span class="repl-error">Failed to load REPL: ' +
        err.message + '</span>';
    });
  }

  function run(go, mod, inst) {
    var decoder = new TextDecoder('utf-8');
    var encoder = new TextEncoder();
    var output = document.getElementById('repl-output');
    var inputEl = document.getElementById('repl-input');

    // Hook fstat so os.Stdin.Stat() reports a character device (terminal).
    // Without this, fstat returns ENOSYS, Stat() returns (nil, err), and
    // gljmain panics on nil.Mode().
    globalThis.fs.fstat = function(fd, callback) {
      callback(null, { dev: 0, ino: 0, mode: 8592, nlink: 0, uid: 0, gid: 0,
                        rdev: 0, size: 0, blksize: 0, blocks: 0,
                        atimeMs: 0, mtimeMs: 0, ctimeMs: 0,
                        isDirectory: function() { return false; },
                        isFile: function() { return false; },
                        isBlockDevice: function() { return false; },
                        isCharacterDevice: function() { return fd < 3; },
                        isSymbolicLink: function() { return false; },
                        isFIFO: function() { return false; },
                        isSocket: function() { return false; } });
    };

    // Hook stdout/stderr: insert text before the input element
    globalThis.fs.writeSync = function(fd, buf) {
      var text = decoder.decode(buf);
      var span = document.createElement('span');
      span.innerText = text;
      output.insertBefore(span, inputEl);
      inputEl.scrollIntoView({ block: 'nearest' });
      return buf.length;
    };

    // Hook stdin: wait for user input via contenteditable
    var pendingRead = null;
    var originalRead = globalThis.fs.read;
    globalThis.fs.read = function(fd, buffer, offset, length, position, callback) {
      if (fd !== 0) {
        return originalRead(fd, buffer, offset, length, position, callback);
      }
      if (pendingRead) {
        throw new Error('multiple reads');
      }
      pendingRead = { buffer: buffer, offset: offset, length: length,
                      position: position, callback: callback };
    };

    // History
    var history = [];
    var historyIndex = 0;
    var currentLine = '';
    var exprBuf = '';

    function nestingDepth(text) {
      var depth = 0;
      var inString = false;
      var escaped = false;
      for (var i = 0; i < text.length; i++) {
        var ch = text[i];
        if (escaped) { escaped = false; continue; }
        if (ch === '\\') { escaped = true; continue; }
        if (ch === '"') { inString = !inString; continue; }
        if (inString) continue;
        if (ch === ';') { while (i < text.length && text[i] !== '\n') i++; continue; }
        if (ch === '(' || ch === '[' || ch === '{') depth++;
        else if (ch === ')' || ch === ']' || ch === '}') depth--;
      }
      return Math.max(0, depth);
    }

    inputEl.addEventListener('paste', function(event) {
      event.preventDefault();
      var text = (event.clipboardData || window.clipboardData).getData('text/plain');
      var lines = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
      // Remove trailing empty lines
      while (lines.length > 0 && lines[lines.length - 1].trim() === '') {
        lines.pop();
      }
      if (lines.length === 0) return;
      if (lines.length === 1) {
        // Single line: insert at cursor
        document.execCommand('insertText', false, lines[0]);
      } else {
        // Multi-line: feed each line through the REPL as if typed + Enter
        for (var i = 0; i < lines.length; i++) {
          inputEl.innerText = lines[i];
          processInput(lines[i]);
          exprBuf += lines[i] + '\n';
        }
        // Pre-fill indent if expression is still incomplete
        var depth = nestingDepth(exprBuf);
        if (depth > 0) {
          var indent = new Array(depth * 2 + 1).join(' ');
          inputEl.innerText = indent;
          var range = document.createRange();
          var sel = window.getSelection();
          range.selectNodeContents(inputEl);
          range.collapse(false);
          sel.removeAllRanges();
          sel.addRange(range);
        } else {
          exprBuf = '';
          inputEl.innerText = '';
        }
        if (text.trim() !== '') {
          history.push(text.trim());
          historyIndex = history.length;
        }
      }
    });

    inputEl.addEventListener('keydown', function(event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        var input = this.innerText;
        this.innerText = '';
        processInput(input);
        if (input.trim() !== '') {
          history.push(input);
        }
        historyIndex = history.length;
        currentLine = '';
        // Smart indent: if expression is incomplete, pre-fill with indentation
        exprBuf += input + '\n';
        var depth = nestingDepth(exprBuf);
        if (depth > 0) {
          var indent = new Array(depth * 2 + 1).join(' ');
          this.innerText = indent;
          // Place cursor at end of indent
          var range = document.createRange();
          var sel = window.getSelection();
          range.selectNodeContents(this);
          range.collapse(false);
          sel.removeAllRanges();
          sel.addRange(range);
        } else {
          exprBuf = '';
        }
      } else if (event.key === 'ArrowUp' ||
                 (event.ctrlKey && event.key === 'p')) {
        event.preventDefault();
        if (historyIndex > 0) {
          if (historyIndex === history.length) {
            currentLine = this.innerText;
          }
          historyIndex--;
          this.innerText = history[historyIndex];
        }
      } else if (event.key === 'ArrowDown' ||
                 (event.ctrlKey && event.key === 'n')) {
        event.preventDefault();
        if (historyIndex < history.length) {
          historyIndex++;
          if (historyIndex === history.length) {
            this.innerText = currentLine;
          } else {
            this.innerText = history[historyIndex];
          }
        }
      } else if (event.key === 'Tab') {
        // Tab: insert 2 spaces instead of leaving the input
        event.preventDefault();
        document.execCommand('insertText', false, '  ');
      } else if (event.key === 'Backspace') {
        // Smart backspace: delete 2 spaces if cursor is preceded by 2 spaces
        var sel = window.getSelection();
        if (sel.rangeCount > 0) {
          var range = sel.getRangeAt(0);
          var offset = range.startOffset;
          var text = this.firstChild ? this.firstChild.nodeValue || '' : '';
          if (offset >= 2 && text[offset - 1] === ' ' && text[offset - 2] === ' ') {
            event.preventDefault();
            this.firstChild.nodeValue = text.slice(0, offset - 2) + text.slice(offset);
            var newRange = document.createRange();
            newRange.setStart(this.firstChild, offset - 2);
            newRange.collapse(true);
            sel.removeAllRanges();
            sel.addRange(newRange);
          }
        }
      } else if (event.key === 'a' && event.ctrlKey) {
        // Ctrl-A: move cursor to beginning of line
        event.preventDefault();
        var sel = window.getSelection();
        var range = document.createRange();
        range.selectNodeContents(inputEl);
        range.collapse(true);
        sel.removeAllRanges();
        sel.addRange(range);
      } else if (event.key === 'e' && event.ctrlKey) {
        // Ctrl-E: move cursor to end of line
        event.preventDefault();
        var sel = window.getSelection();
        var range = document.createRange();
        range.selectNodeContents(inputEl);
        range.collapse(false);
        sel.removeAllRanges();
        sel.addRange(range);
      } else if (event.key === 'u' && event.ctrlKey) {
        // Ctrl-U: delete from cursor to beginning of line
        event.preventDefault();
        var sel = window.getSelection();
        if (sel.rangeCount > 0) {
          var range = sel.getRangeAt(0);
          var delRange = document.createRange();
          delRange.selectNodeContents(inputEl);
          delRange.setEnd(range.startContainer, range.startOffset);
          delRange.deleteContents();
        }
      } else if (event.key === 'k' && event.ctrlKey) {
        // Ctrl-K: delete from cursor to end of line
        event.preventDefault();
        var sel = window.getSelection();
        if (sel.rangeCount > 0) {
          var range = sel.getRangeAt(0);
          var delRange = document.createRange();
          delRange.setStart(range.startContainer, range.startOffset);
          var endRange = document.createRange();
          endRange.selectNodeContents(inputEl);
          delRange.setEnd(endRange.endContainer, endRange.endOffset);
          delRange.deleteContents();
        }
      } else if (event.key === 'l' && event.ctrlKey) {
        // Ctrl-L: clear screen, then re-add the prompt
        event.preventDefault();
        var spans = output.querySelectorAll('span:not(#repl-input)');
        spans.forEach(function(s) { s.remove(); });
        var prompt = document.createElement('span');
        prompt.innerText = 'user=> ';
        output.insertBefore(prompt, inputEl);
      }
    });

    function processInput(input) {
      // Echo the input line
      var span = document.createElement('span');
      span.innerText = input + '\n';
      output.insertBefore(span, inputEl);

      if (pendingRead) {
        var pr = pendingRead;
        pendingRead = null;
        var view = encoder.encode(input + '\n');
        pr.buffer.set(view, pr.offset);
        pr.callback(null, view.length);
      }
    }

    // Show the terminal, hide loading
    document.getElementById('repl-loading').style.display = 'none';
    document.getElementById('repl-container').style.display = 'block';
    inputEl.focus();

    // Print Gloat banner line (matches bin/gloat output)
    var gloatBanner = document.createElement('span');
    gloatBanner.innerText = '🐐 Gloat: 0.1.34 🐐\n';
    output.insertBefore(gloatBanner, inputEl);

    go.run(inst).then(function() {
      // Reset if WASM exits
      WebAssembly.instantiate(mod, go.importObject).then(function(newInst) {
        run(go, mod, newInst.instance);
      });
    });
  }

  // Subscribe to Material's instant navigation
  if (typeof document$ !== 'undefined') {
    document$.subscribe(function() { initRepl(); });
  } else {
    document.addEventListener('DOMContentLoaded', initRepl);
  }
})();

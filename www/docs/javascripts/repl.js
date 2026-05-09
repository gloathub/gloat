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

    // Suppress whitespace-only WASM output during multi-line input
    var suppressContinuation = false;

    // Hook stdout/stderr: insert text before the input element
    globalThis.fs.writeSync = function(fd, buf) {
      var text = decoder.decode(buf);
      if (suppressContinuation) {
        if (text.trim() === '') return buf.length;
        suppressContinuation = false;
      }
      var span = document.createElement('span');
      span.innerText = text;
      output.insertBefore(span, inputEl);
      inputEl.scrollIntoView({ block: 'nearest' });
      return buf.length;
    };

    // Queue for replaying shared URL expressions
    var replayQueue = [];
    var replayLast = '';
    var hash = window.location.hash.slice(1);
    if (hash.indexOf('s:') === 0) {
      try {
        var json = decodeURIComponent(escape(atob(hash.slice(2))));
        var exprs = JSON.parse(json);
        if (exprs.length > 0) {
          replayLast = exprs.pop();
          replayQueue = exprs;
        }
      } catch(e) {}
    }

    // Hook stdin: wait for user input via contenteditable
    var pendingRead = null;
    var originalRead = globalThis.fs.read;
    globalThis.fs.read = function(fd, buffer, offset, length, position, callback) {
      if (fd !== 0) {
        return originalRead(fd, buffer, offset, length, position, callback);
      }
      // Replay queued expressions from shared URL
      if (replayQueue.length > 0) {
        var expr = replayQueue.shift();
        var span = document.createElement('span');
        span.innerHTML = highlightSyntax(expr) + '\n';
        output.insertBefore(span, inputEl);
        history.push(expr);
        historyIndex = history.length;
        if (expr.indexOf('\n') >= 0) suppressContinuation = true;
        var view = encoder.encode(expr + '\n');
        buffer.set(view, offset);
        callback(null, view.length);
        return;
      }
      // Place last shared expression in input for user to run
      if (replayLast) {
        setInput(replayLast);
        inputEl.scrollIntoView({ block: 'nearest' });
        replayLast = '';
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

    // Remove common leading whitespace from multi-line text
    function dedent(text) {
      var lines = text.split('\n');
      var min = Infinity;
      for (var i = 0; i < lines.length; i++) {
        if (lines[i].trim() === '') continue;
        var m = lines[i].match(/^( *)/);
        if (m && m[1].length < min) min = m[1].length;
      }
      if (min === 0 || min === Infinity) return text;
      return lines.map(function(l) { return l.slice(min); }).join('\n');
    }

    // Set input element content with highlighting, multi-line on new line
    function setInput(text) {
      var isMulti = text.indexOf('\n') >= 0;
      inputEl.style.display = isMulti ? 'block' : 'inline-block';
      inputEl.innerHTML = highlightSyntax(isMulti ? dedent(text) : text);
      // Place cursor at end
      var sel = window.getSelection();
      var range = document.createRange();
      range.selectNodeContents(inputEl);
      range.collapse(false);
      sel.removeAllRanges();
      sel.addRange(range);
    }

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

    // Live syntax highlighting as user types
    inputEl.addEventListener('input', function() {
      var text = this.innerText;
      if (text === '') return;
      // Save cursor offset
      var sel = window.getSelection();
      var offset = 0;
      if (sel.rangeCount > 0) {
        var range = sel.getRangeAt(0);
        // Walk text nodes to compute absolute offset
        var walker = document.createTreeWalker(this, NodeFilter.SHOW_TEXT, null, false);
        var node;
        while ((node = walker.nextNode())) {
          if (node === range.startContainer) {
            offset += range.startOffset;
            break;
          }
          offset += node.length;
        }
      }
      // Replace with highlighted HTML
      this.innerHTML = highlightSyntax(text);
      // Restore cursor position
      if (offset > 0) {
        var walker = document.createTreeWalker(this, NodeFilter.SHOW_TEXT, null, false);
        var node;
        var remaining = offset;
        while ((node = walker.nextNode())) {
          if (remaining <= node.length) {
            var newRange = document.createRange();
            newRange.setStart(node, remaining);
            newRange.collapse(true);
            sel.removeAllRanges();
            sel.addRange(newRange);
            break;
          }
          remaining -= node.length;
        }
      }
    });

    inputEl.addEventListener('paste', function(event) {
      event.preventDefault();
      var text = (event.clipboardData || window.clipboardData).getData('text/plain');
      // Strip trailing newlines and insert at cursor
      text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/\n+$/, '');
      if (text) document.execCommand('insertText', false, text);
    });

    inputEl.addEventListener('keydown', function(event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        var input = this.innerText;
        this.innerText = '';
        this.style.display = 'inline-block';
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
          setInput(history[historyIndex]);
        }
      } else if (event.key === 'ArrowDown' ||
                 (event.ctrlKey && event.key === 'n')) {
        event.preventDefault();
        if (historyIndex < history.length) {
          historyIndex++;
          if (historyIndex === history.length) {
            setInput(currentLine);
          } else {
            setInput(history[historyIndex]);
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
        // Ctrl-L: clear screen
        event.preventDefault();
        handleAction('clear');
      }
      inputEl.scrollIntoView({ block: 'nearest' });
      if (typeof updateShareUrl === 'function') updateShareUrl();
    });

    var coreSymbols = {
      '+':1, '-':1, '*':1, '/':1, '<':1, '>':1, '<=':1, '>=':1, '=':1,
      '==':1, 'not=':1, 'not':1, 'and':1, 'or':1, 'mod':1, 'rem':1,
      'inc':1, 'dec':1, 'max':1, 'min':1, 'abs':1, 'quot':1,
      'zero?':1, 'pos?':1, 'neg?':1, 'even?':1, 'odd?':1, 'number?':1,
      'nil?':1, 'true?':1, 'false?':1, 'some?':1, 'empty?':1,
      'string?':1, 'keyword?':1, 'symbol?':1, 'map?':1, 'vector?':1,
      'list?':1, 'set?':1, 'seq?':1, 'fn?':1, 'coll?':1, 'sequential?':1,
      'associative?':1, 'counted?':1, 'sorted?':1, 'reversible?':1,
      'int?':1, 'integer?':1, 'float?':1, 'double?':1, 'rational?':1,
      'identical?':1, 'instance?':1, 'satisfies?':1, 'isa?':1,
      'contains?':1, 'every?':1, 'not-every?':1, 'some':1, 'not-any?':1,
      'realized?':1, 'future?':1, 'bound?':1, 'thread-bound?':1,
      'str':1, 'name':1, 'namespace':1, 'keyword':1, 'symbol':1,
      'gensym':1, 'intern':1, 'type':1, 'class':1, 'supers':1,
      'pr':1, 'prn':1, 'print':1, 'println':1, 'pr-str':1, 'prn-str':1,
      'print-str':1, 'format':1, 'printf':1, 'newline':1,
      'read-string':1, 'read-line':1, 'slurp':1, 'spit':1,
      'first':1, 'second':1, 'last':1, 'rest':1, 'next':1, 'ffirst':1,
      'fnext':1, 'nfirst':1, 'nnext':1, 'nth':1, 'nthrest':1, 'nthnext':1,
      'cons':1, 'conj':1, 'concat':1, 'into':1, 'seq':1, 'sequence':1,
      'lazy-seq':1, 'lazy-cat':1, 'list':1, 'list*':1, 'vector':1,
      'vec':1, 'hash-map':1, 'array-map':1, 'sorted-map':1, 'sorted-map-by':1,
      'hash-set':1, 'sorted-set':1, 'sorted-set-by':1, 'set':1,
      'count':1, 'range':1, 'repeat':1, 'repeatedly':1, 'iterate':1,
      'cycle':1, 'interleave':1, 'interpose':1, 'flatten':1,
      'map':1, 'mapv':1, 'map-indexed':1, 'mapcat':1, 'pmap':1,
      'filter':1, 'filterv':1, 'remove':1, 'keep':1, 'keep-indexed':1,
      'reduce':1, 'reduce-kv':1, 'reductions':1, 'transduce':1,
      'apply':1, 'comp':1, 'partial':1, 'complement':1, 'juxt':1,
      'identity':1, 'constantly':1, 'memoize':1, 'fnil':1,
      'take':1, 'take-while':1, 'take-nth':1, 'take-last':1,
      'drop':1, 'drop-while':1, 'drop-last':1,
      'sort':1, 'sort-by':1, 'reverse':1, 'shuffle':1,
      'group-by':1, 'partition':1, 'partition-by':1, 'partition-all':1,
      'split-at':1, 'split-with':1, 'frequencies':1, 'distinct':1,
      'dedupe':1, 'zipmap':1,
      'get':1, 'get-in':1, 'assoc':1, 'assoc-in':1, 'dissoc':1,
      'update':1, 'update-in':1, 'select-keys':1, 'find':1,
      'keys':1, 'vals':1, 'merge':1, 'merge-with':1,
      'compare':1, 'hash':1,
      'atom':1, 'deref':1, 'reset!':1, 'swap!':1, 'compare-and-set!':1,
      'add-watch':1, 'remove-watch':1, 'ref':1, 'dosync':1,
      'agent':1, 'send':1, 'send-off':1, 'await':1,
      'future':1, 'promise':1, 'deliver':1,
      'delay':1, 'force':1,
      'with-meta':1, 'meta':1, 'vary-meta':1, 'alter-meta!':1,
      'the-ns':1, 'find-ns':1, 'create-ns':1, 'remove-ns':1,
      'ns-name':1, 'ns-map':1, 'ns-publics':1, 'ns-imports':1,
      'ns-interns':1, 'ns-refers':1, 'ns-aliases':1, 'ns-resolve':1,
      'resolve':1, 'all-ns':1,
      'eval':1, 'load':1, 'load-file':1, 'load-string':1,
      'macroexpand':1, 'macroexpand-1':1,
      'rand':1, 'rand-int':1, 'rand-nth':1,
      'with-out-str':1, 'with-in-str':1,
      'doall':1, 'dorun':1, 'doto':1, 'bean':1,
      'assert':1, 'time':1, 'doc':1, 'source':1,
      'cond->':1, 'cond->>':1, 'as->':1, 'some->':1, 'some->>':1,
      '->':1, '->>':1
    };

    var specialForms = {
      'def':1, 'defn':1, 'defn-':1, 'defmacro':1, 'defonce':1,
      'defmethod':1, 'defmulti':1, 'defprotocol':1, 'defrecord':1,
      'deftype':1, 'defstruct':1, 'fn':1, 'fn*':1, 'let':1,
      'let*':1, 'loop':1, 'recur':1, 'if':1, 'if-let':1,
      'if-not':1, 'when':1, 'when-let':1, 'when-not':1,
      'when-first':1, 'cond':1, 'condp':1, 'case':1, 'do':1,
      'quote':1, 'var':1, 'try':1, 'catch':1, 'finally':1,
      'throw':1, 'ns':1, 'require':1, 'import':1, 'use':1,
      'refer':1, 'in-ns':1, 'for':1, 'doseq':1, 'dotimes':1,
      'while':1, 'binding':1, 'with-open':1, 'with-local-vars':1
    };

    function isSymbolChar(ch) {
      if (ch >= 'a' && ch <= 'z') return true;
      if (ch >= 'A' && ch <= 'Z') return true;
      if (ch >= '0' && ch <= '9') return true;
      return '.*+!-_?/<>=$&%#:'.indexOf(ch) >= 0;
    }

    function isNumber(s) {
      if (s.length === 0) return false;
      var i = 0;
      if (s[0] === '-' || s[0] === '+') {
        i++;
        if (i >= s.length) return false;
      }
      return s[i] >= '0' && s[i] <= '9';
    }

    function highlightSyntax(text) {
      var out = '';
      var i = 0;
      var n = text.length;

      function esc(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      }

      function wrap(cls, s) {
        return '<span class="hl-' + cls + '">' + esc(s) + '</span>';
      }

      while (i < n) {
        var ch = text[i];

        // String literal
        if (ch === '"') {
          var start = i;
          i++;
          while (i < n) {
            if (text[i] === '\\' && i + 1 < n) { i += 2; continue; }
            if (text[i] === '"') { i++; break; }
            i++;
          }
          out += wrap('string', text.slice(start, i));
          continue;
        }

        // Comment
        if (ch === ';') {
          var start = i;
          while (i < n && text[i] !== '\n') i++;
          out += wrap('comment', text.slice(start, i));
          continue;
        }

        // Keyword
        if (ch === ':' && (i === 0 || !isSymbolChar(text[i - 1]))) {
          var start = i;
          i++;
          if (i < n && text[i] === ':') i++;
          while (i < n && isSymbolChar(text[i])) i++;
          out += wrap('keyword', text.slice(start, i));
          continue;
        }

        // Delimiters, whitespace, special chars - pass through
        if ('()[]{}\'`@^~#,'.indexOf(ch) >= 0 ||
            ch === ' ' || ch === '\t' || ch === '\n' || ch === '\r') {
          out += esc(ch);
          i++;
          continue;
        }

        // Symbol or number token
        var start = i;
        while (i < n && isSymbolChar(text[i])) i++;
        if (i === start) {
          out += esc(ch);
          i++;
          continue;
        }

        var token = text.slice(start, i);

        if (token === 'true' || token === 'false' || token === 'nil') {
          out += wrap('literal', token);
        } else if (specialForms[token]) {
          out += wrap('special', token);
        } else if (isNumber(token)) {
          out += wrap('literal', token);
        } else if (coreSymbols[token]) {
          out += wrap('core', token);
        } else {
          out += wrap('symbol', token);
        }
      }
      return out;
    }

    function processInput(input) {
      var isMultiLine = input.indexOf('\n') >= 0;
      // Echo the input with syntax highlighting
      var span = document.createElement('span');
      if (isMultiLine) span.style.display = 'block';
      span.innerHTML = highlightSyntax(input) + '\n';
      output.insertBefore(span, inputEl);

      // Suppress WASM continuation prompts for multi-line input
      if (isMultiLine) suppressContinuation = true;

      if (pendingRead) {
        var pr = pendingRead;
        pendingRead = null;
        // Strip trailing newlines to avoid extra empty line → double prompt
        var cleaned = input.replace(/\n+$/, '');
        var view = encoder.encode(cleaned + '\n');
        pr.buffer.set(view, pr.offset);
        pr.callback(null, view.length);
      }
    }

    // Show the terminal and toolbar, hide loading
    document.getElementById('repl-loading').style.display = 'none';
    document.getElementById('repl-toolbar').style.display = 'flex';
    document.getElementById('repl-container').style.display = 'block';
    inputEl.focus();

    // Toolbar action handler (shared by inline buttons and dropdown)
    function handleAction(action) {
      if (action === 'clear') {
        var spans = output.querySelectorAll('span:not(#repl-input)');
        spans.forEach(function(s) { s.remove(); });
        var prompt = document.createElement('span');
        prompt.innerText = 'user=> ';
        output.insertBefore(prompt, inputEl);
        historyIndex = history.length;
        currentLine = '';
      } else if (action === 'history-prev') {
        if (historyIndex > 0) {
          if (historyIndex === history.length) {
            currentLine = inputEl.innerText;
          }
          historyIndex--;
          setInput(history[historyIndex]);
        }
      } else if (action === 'history-next') {
        if (historyIndex < history.length) {
          historyIndex++;
          if (historyIndex === history.length) {
            setInput(currentLine);
          } else {
            setInput(history[historyIndex]);
          }
        }
      } else if (action === 'home') {
        var sel = window.getSelection();
        var range = document.createRange();
        range.selectNodeContents(inputEl);
        range.collapse(true);
        sel.removeAllRanges();
        sel.addRange(range);
      } else if (action === 'end') {
        var sel = window.getSelection();
        var range = document.createRange();
        range.selectNodeContents(inputEl);
        range.collapse(false);
        sel.removeAllRanges();
        sel.addRange(range);
      } else if (action === 'kill-before') {
        var sel = window.getSelection();
        if (sel.rangeCount > 0) {
          var range = sel.getRangeAt(0);
          var delRange = document.createRange();
          delRange.selectNodeContents(inputEl);
          delRange.setEnd(range.startContainer, range.startOffset);
          delRange.deleteContents();
        }
      } else if (action === 'kill-after') {
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
      } else if (action === 'copy-form') {
        // Copy current form: input text, history entry, or last form
        var text = inputEl.innerText.trim();
        if (!text && historyIndex < history.length) {
          text = history[historyIndex];
        }
        if (!text && history.length > 0) {
          text = history[history.length - 1];
        }
        if (text) navigator.clipboard.writeText(text);
      } else if (action === 'share') {
        navigator.clipboard.writeText(window.location.href);
      }
      inputEl.focus();
      inputEl.scrollIntoView({ block: 'nearest' });
    }

    // Inline toolbar buttons
    document.querySelector('.repl-buttons').addEventListener('click', function(e) {
      var btn = e.target.closest('button[data-action]');
      if (!btn) return;
      handleAction(btn.getAttribute('data-action'));
    });

    // Keep URL hash in sync with share state
    function updateShareUrl() {
      var exprs = history.slice(historyIndex);
      if (historyIndex === history.length) {
        var current = inputEl.innerText;
        if (current.trim()) exprs.push(current);
      }
      var base = window.location.href.replace(/#.*$/, '');
      if (exprs.length > 0) {
        var b64 = btoa(unescape(encodeURIComponent(JSON.stringify(exprs))));
        window.history.replaceState(null, '', base + '#s:' + b64);
      } else {
        window.history.replaceState(null, '', base);
      }
    }
    inputEl.addEventListener('input', updateShareUrl);

    // Dropdown menu (if present)
    var ctrlMenu = document.getElementById('repl-ctrl-menu');
    if (ctrlMenu) {
      var menuBtn = ctrlMenu.querySelector('.repl-menu-btn');
      menuBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        ctrlMenu.classList.toggle('open');
      });
      document.addEventListener('click', function() {
        ctrlMenu.classList.remove('open');
      });
      ctrlMenu.querySelector('.repl-menu-items').addEventListener('click', function(e) {
        var btn = e.target.closest('button[data-action]');
        if (!btn) return;
        e.stopPropagation();
        ctrlMenu.classList.remove('open');
        handleAction(btn.getAttribute('data-action'));
      });
    }

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

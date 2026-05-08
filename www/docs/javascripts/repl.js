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

    inputEl.addEventListener('keydown', function(event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        var input = this.innerText.trim();
        this.innerText = '';
        processInput(input);
        if (input !== '') {
          history.push(input);
        }
        historyIndex = history.length;
        currentLine = '';
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
      } else if (event.key === 'l' && event.ctrlKey) {
        // Ctrl-L: clear screen
        event.preventDefault();
        var spans = output.querySelectorAll('span:not(#repl-input)');
        spans.forEach(function(s) { s.remove(); });
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

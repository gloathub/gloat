# Gloat Interactive Web Editor

An interactive web-based editor for compiling YAMLScript and Clojure to WASM via
Glojure and Go.

## Quick Start

```bash
cd example
make serve
```

Then open http://localhost:8080 in your browser.

## Features

- **File Selector**: Choose from available `.ys` and `.clj` files in
  `yamlscript/` and `clojure/` directories
- **Live Editor**: Edit source code in CodeMirror 6 with syntax highlighting
- **On-Demand Compilation**: Click "Run" to compile and execute
- **Multi-Stage Pipeline**: View intermediate Clojure, Glojure, and Go code
- **WASM Execution**: Runs compiled WASM in the browser

## How It Works

### Server (bin/server.py)

Python HTTP server providing:

- `GET /api/files` - Lists available source files
- `GET /api/config` - Returns config.json settings
- `GET /api/source/:path` - Returns source file content
- `POST /api/compile` - Compiles source to all intermediate formats + WASM
- `GET /` - Serves index.html
- `GET /*` - Serves static files (wasm_exec.js, etc.)

### Frontend (index.html)

- **CodeMirror 6** editor for YAMLScript/Clojure source
- File selector dropdown populated from server
- Run button that:
  1. Sends editor content to `/api/compile`
  2. Updates Glojure and Go code panels on success
  3. Loads and executes WASM binary
  4. Displays compilation errors in output pane on failure

## Architecture

```
User edits source in CodeMirror
        ↓
Click "Run"
        ↓
POST /api/compile {source, ext}
        ↓
Server invokes gloat via bin/compile.sh:
  - gloat temp.ys -t clj
  - gloat temp.ys -t glj
  - gloat temp.ys -t go
  - gloat temp.ys -o temp.wasm -t js
        ↓
Server streams progress via SSE (Server-Sent Events)
        ↓
Server returns {clj, glj, go, wasm (base64)}
        ↓
Frontend updates code panels and runs WASM
```

## Files

- `bin/server.py` - Python HTTP server with compilation API
- `bin/compile.sh` - Compilation helper script invoked by server
- `bin/config.jq` - JQ config processor for argument presets
- `index.html` - Interactive web UI with CodeMirror editor
- `config.yaml` / `config.json` - Program argument presets
- `yamlscript/` - YAMLScript example programs
- `clojure/` - Clojure example programs
- `Makefile` - Build targets including `make serve`
- `wasm_exec.js` - Go's WASM runtime (auto-generated)

## Development

The server uses Python's built-in HTTP server and invokes the gloat compiler via
`make shell` for each compilation request.
All compilation happens server-side in temporary directories that are cleaned up
after each request.

## Notes

- Requires Python, Go, Glojure, and YAMLScript (installed via Makes)
- Compilation can take a few seconds for the first request
- Server uses SSE (Server-Sent Events) to stream real-time progress updates
- WASM files are base64-encoded for transport
- Arguments can be passed via the text input field
- Argument presets can be configured in config.yaml/config.json

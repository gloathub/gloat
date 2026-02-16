#!/usr/bin/env python3

import http.server
import socketserver
import json
import subprocess
import tempfile
import os
import base64
import time
from pathlib import Path
from urllib.parse import urlparse, parse_qs

PORT = 8080
EXAMPLE_DIR = Path(__file__).parent.parent  # example/bin/server.py -> example/
PROJECT_DIR = EXAMPLE_DIR.parent            # example/ -> gloat/

class GloatHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        parsed_path = urlparse(self.path)
        path = parsed_path.path

        # API: List files
        if path == '/api/files':
            self.send_json_response(self.list_files())

        # API: Get config
        elif path == '/api/config':
            self.send_json_response(self.load_config())

        # API: Get source
        elif path.startswith('/api/source/'):
            source_path = path[12:]  # Remove '/api/source/'
            self.serve_source(source_path)

        # Default: serve static files
        else:
            super().do_GET()

    def do_POST(self):
        if self.path == '/api/compile':
            content_length = int(self.headers['Content-Length'])
            body = self.rfile.read(content_length)
            data = json.loads(body)

            # Send SSE headers for streaming progress
            self.send_response(200)
            self.send_header('Content-Type', 'text/event-stream')
            self.send_header('Cache-Control', 'no-cache')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()

            # Stream compilation progress
            self.compile_source_stream(data['source'], data['ext'])
        else:
            self.send_error(404)

    def load_config(self):
        config_file = EXAMPLE_DIR / "config.json"
        if config_file.exists():
            with open(config_file, 'r') as f:
                return json.load(f)
        return {}

    def list_files(self):
        files = []

        # List YAMLScript files
        ys_dir = EXAMPLE_DIR / "yamlscript"
        if ys_dir.exists():
            files.extend([f"yamlscript/{f.name}" for f in ys_dir.glob("*.ys")])

        # List Clojure files
        clj_dir = EXAMPLE_DIR / "clojure"
        if clj_dir.exists():
            files.extend([f"clojure/{f.name}" for f in clj_dir.glob("*.clj")])

        return sorted(files)

    def serve_source(self, source_path):
        file_path = EXAMPLE_DIR / source_path

        if file_path.exists() and file_path.is_file():
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(file_path.read_bytes())
        else:
            self.send_error(404, "File not found")

    def compile_source(self, source, ext):
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            source_file = temp_path / f"temp{ext}"
            wasm_file = temp_path / "temp.wasm"

            # Write source to temp file
            source_file.write_text(source)

            try:
                print(f"Compiling {source_file}...")

                compile_script = EXAMPLE_DIR / 'bin' / 'compile.sh'

                # Compile to intermediates
                clj_result = subprocess.run(
                    [str(compile_script), str(source_file), '', 'clj'],
                    capture_output=True,
                    text=True,
                    timeout=30
                )
                print(f"CLJ result: exit={clj_result.returncode}")
                if clj_result.returncode != 0:
                    print(f"CLJ stderr: {clj_result.stderr}")

                glj_result = subprocess.run(
                    [str(compile_script), str(source_file), '', 'glj'],
                    capture_output=True,
                    text=True,
                    timeout=30
                )
                print(f"GLJ result: exit={glj_result.returncode}")
                if glj_result.returncode != 0:
                    print(f"GLJ stderr: {glj_result.stderr}")

                go_result = subprocess.run(
                    [str(compile_script), str(source_file), '', 'go'],
                    capture_output=True,
                    text=True,
                    timeout=30
                )
                print(f"GO result: exit={go_result.returncode}")
                if go_result.returncode != 0:
                    print(f"GO stderr: {go_result.stderr}")

                wasm_result = subprocess.run(
                    [str(compile_script), str(source_file), str(wasm_file), 'js'],
                    capture_output=True,
                    text=True,
                    timeout=90
                )
                print(f"WASM result: exit={wasm_result.returncode}")
                print(f"WASM stdout: {wasm_result.stdout[:500] if wasm_result.stdout else 'empty'}")
                print(f"WASM stderr: {wasm_result.stderr[:500] if wasm_result.stderr else 'empty'}")

                # Debug: list files in temp directory
                temp_files = list(temp_path.iterdir())
                print(f"Files in temp dir: {[f.name for f in temp_files]}")

                # Check if all compilations succeeded
                if all(r.returncode == 0 for r in [clj_result, glj_result, go_result, wasm_result]):
                    # Read WASM and encode as base64
                    if not wasm_file.exists():
                        print(f"WASM file not found: {wasm_file}")
                        print(f"Looking for .js file instead...")
                        js_file = wasm_file.with_suffix('.js')
                        if js_file.exists():
                            print(f"Found .js file: {js_file}")
                            # The .js file is the WASM JavaScript wrapper, we need the actual .wasm
                            # which might be in the same directory
                            actual_wasm = temp_path / (wasm_file.stem + '.wasm')
                            if actual_wasm.exists():
                                wasm_file = actual_wasm
                                print(f"Using WASM file: {wasm_file}")

                    wasm_bytes = wasm_file.read_bytes()
                    wasm_base64 = base64.b64encode(wasm_bytes).decode('utf-8')

                    return {
                        'success': True,
                        'clj': clj_result.stdout,
                        'glj': glj_result.stdout,
                        'go': go_result.stdout,
                        'wasm': wasm_base64
                    }
                else:
                    # Return first error found with more details
                    errors = []
                    if clj_result.returncode != 0:
                        errors.append(f"CLJ: {clj_result.stderr}")
                    if glj_result.returncode != 0:
                        errors.append(f"GLJ: {glj_result.stderr}")
                    if go_result.returncode != 0:
                        errors.append(f"GO: {go_result.stderr}")
                    if wasm_result.returncode != 0:
                        errors.append(f"WASM: {wasm_result.stderr}")

                    error = "\n".join(errors) if errors else "Unknown compilation error"
                    print(f"Compilation failed: {error}")
                    return {'success': False, 'error': error}

            except subprocess.TimeoutExpired as e:
                print(f"Compilation timeout: {e}")
                return {'success': False, 'error': f'Compilation timeout after {e.timeout}s'}
            except Exception as e:
                print(f"Compilation exception: {e}")
                return {'success': False, 'error': str(e)}

    def send_sse(self, event, data):
        """Send a Server-Sent Event"""
        message = f"event: {event}\ndata: {json.dumps(data)}\n\n"
        self.wfile.write(message.encode('utf-8'))
        self.wfile.flush()

    def compile_source_stream(self, source, ext):
        """Compile source and stream progress via SSE"""
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            source_file = temp_path / f"temp{ext}"
            wasm_file = temp_path / "temp.wasm"

            source_file.write_text(source)

            try:
                compile_script = EXAMPLE_DIR / 'bin' / 'compile.sh'

                # CLJ step
                self.send_sse('progress', {'step': 'clj', 'status': 'started'})
                clj_start = time.time()
                clj_result = subprocess.run(
                    [str(compile_script), str(source_file), '', 'clj'],
                    capture_output=True, text=True, timeout=30
                )
                clj_ms = int((time.time() - clj_start) * 1000)
                if clj_result.returncode != 0:
                    self.send_sse('error', {'step': 'clj', 'error': clj_result.stderr})
                    return
                self.send_sse('progress', {'step': 'clj', 'status': 'done', 'ms': clj_ms})

                # GLJ step
                self.send_sse('progress', {'step': 'glj', 'status': 'started'})
                glj_start = time.time()
                glj_result = subprocess.run(
                    [str(compile_script), str(source_file), '', 'glj'],
                    capture_output=True, text=True, timeout=30
                )
                glj_ms = int((time.time() - glj_start) * 1000)
                if glj_result.returncode != 0:
                    self.send_sse('error', {'step': 'glj', 'error': glj_result.stderr})
                    return
                self.send_sse('progress', {'step': 'glj', 'status': 'done', 'ms': glj_ms, 'code': glj_result.stdout})

                # GO step
                self.send_sse('progress', {'step': 'go', 'status': 'started'})
                go_start = time.time()
                go_result = subprocess.run(
                    [str(compile_script), str(source_file), '', 'go'],
                    capture_output=True, text=True, timeout=30
                )
                go_ms = int((time.time() - go_start) * 1000)
                if go_result.returncode != 0:
                    self.send_sse('error', {'step': 'go', 'error': go_result.stderr})
                    return
                self.send_sse('progress', {'step': 'go', 'status': 'done', 'ms': go_ms, 'code': go_result.stdout})

                # WASM step
                self.send_sse('progress', {'step': 'wasm', 'status': 'started'})
                wasm_start = time.time()
                wasm_result = subprocess.run(
                    [str(compile_script), str(source_file), str(wasm_file), 'js'],
                    capture_output=True, text=True, timeout=90
                )
                wasm_ms = int((time.time() - wasm_start) * 1000)
                if wasm_result.returncode != 0:
                    self.send_sse('error', {'step': 'wasm', 'error': wasm_result.stderr})
                    return
                self.send_sse('progress', {'step': 'wasm', 'status': 'done', 'ms': wasm_ms})

                # Find WASM file
                if not wasm_file.exists():
                    actual_wasm = temp_path / (wasm_file.stem + '.wasm')
                    if actual_wasm.exists():
                        wasm_file = actual_wasm

                # Encode step - read and base64 encode WASM
                self.send_sse('progress', {'step': 'encode', 'status': 'started'})
                encode_start = time.time()

                wasm_bytes = wasm_file.read_bytes()
                wasm_base64 = base64.b64encode(wasm_bytes).decode('utf-8')

                encode_ms = int((time.time() - encode_start) * 1000)
                self.send_sse('progress', {'step': 'encode', 'status': 'done', 'ms': encode_ms})

                self.send_sse('done', {
                    'success': True,
                    'clj': clj_result.stdout,
                    'glj': glj_result.stdout,
                    'go': go_result.stdout,
                    'wasm': wasm_base64
                })

            except subprocess.TimeoutExpired as e:
                self.send_sse('error', {'error': f'Compilation timeout after {e.timeout}s'})
            except Exception as e:
                self.send_sse('error', {'error': str(e)})

    def send_json_response(self, data):
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(data).encode('utf-8'))

if __name__ == '__main__':
    # Debug output
    print(f"Current working directory: {os.getcwd()}")
    print(f"EXAMPLE_DIR: {EXAMPLE_DIR.resolve()}")
    print(f"PROJECT_DIR: {PROJECT_DIR.resolve()}")
    print(f"yamlscript dir exists: {(EXAMPLE_DIR / 'yamlscript').exists()}")
    print(f"clojure dir exists: {(EXAMPLE_DIR / 'clojure').exists()}")

    # Change to example directory to serve static files from there
    os.chdir(EXAMPLE_DIR)
    print(f"Changed to directory: {os.getcwd()}")

    # Create server with address reuse enabled to avoid "Address already in use" errors
    class ReusableTCPServer(socketserver.TCPServer):
        allow_reuse_address = True

    with ReusableTCPServer(("", PORT), GloatHandler) as httpd:
        print(f"Starting server on http://localhost:{PORT}")
        print("Press Ctrl+C to stop")
        httpd.serve_forever()

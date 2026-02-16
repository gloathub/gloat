# Live Demo

## Try Gloat in Your Browser

Click the badge below to launch an interactive Gloat demo in GitHub Codespaces:

<div class="badge">
  <a href="https://codespaces.new/gloathub/gloat?quickstart=1">
    <img src="https://img.shields.io/badge/Try_Gloat-Live_Demo-blue?logo=github" alt="Try Gloat Live Demo">
  </a>
</div>

The demo will open in a VS Code environment and automatically start a web-based
demo server.
You can compile and run demo programs directly in your browser!

!!! warning "Startup Time"
    The Codespaces environment takes **1-2 minutes** to fully initialize.
    You'll see a VS Code editor first, then the demo page will appear in a pane.

    You can use **Cmd/Ctrl + Shift + P → View Creation Log** to see the full
    initialization progress.

## What the Demo Does

The demo server provides:

- **Interactive code editor** - Choose from 50+ demo programs
- **Compile button** - Converts your code to WebAssembly
- **Run button** - Executes the Wasm in your browser
- **Multi-format output** - View generated Glojure and Go code

All compilation happens server-side, and the resulting Wasm runs entirely in
your browser using the WebAssembly System Interface (WASI).

## Running the Demo Locally

If you've cloned the Gloat repository, you can run the demo on your local
machine:

```bash
# Clone and setup
git clone https://github.com/gloathub/gloat
cd gloat
source .rc

# Start the demo server
make demo-server
```

This will start a local server at `http://localhost:8080` with the same
interactive interface as the Codespaces version.

## After the Demo

Once you've tried the demo, check out:

- [Getting Started](getting-started.md) - Install Gloat locally
- [Examples](examples.md) - Browse all demo programs
- [GitHub Repository](https://github.com/gloathub/gloat) - Source code and
  documentation

---

!!! note "Future Plans"
    We're working on a **fully static demo** that will run without requiring a
    server. This will enable instant experimentation without the Codespaces
    startup delay.

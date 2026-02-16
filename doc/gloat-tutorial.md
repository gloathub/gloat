# Gloat Ecosystem Tutorial

This document explains how the gloat ecosystem works, covering all four
interconnected repositories and their roles in building YAMLScript applications
that compile to native Go binaries.

## Table of Contents

- [Overview](#overview)
- [Repository Layout](#repository-layout)
- [The Makes Build System](#the-makes-build-system)
- [How Gloat Generates Output](#how-gloat-generates-output)
- [Downstream Project Setup](#downstream-project-setup)
- [Version Management](#version-management)
- [The YS Standard Library](#the-ys-standard-library)
- [The Glojure Fork](#the-glojure-fork)
- [Development Workflow](#development-workflow)

## Overview

### The Four Repositories

The gloat ecosystem consists of four interconnected repositories:

1. **gloathub/gloat** — The AOT (Ahead-of-Time) compiler
   - Converts YAMLScript, Clojure, or Glojure source files to Go code
   - Provides the YS standard library as a Go module (`ys/pkg`)
   - Generates buildable Go directories or compiled binaries

2. **gloathub/glojure** — Forked Glojure compiler with gloat-specific patches
   - Based on the upstream glojurelang/glojure project
   - Includes patches for improved Go compilation
   - Adds 15-20 additional cross-compilation targets beyond upstream
   - Fixes for issues like `seqable?` handling

3. **makeplus/makes** — Makefile-based dependency and build system
   - Auto-bootstraps all tool dependencies
   - Provides `.mk` modules for gloat, glojure, go, yamlscript, etc.
   - Installs everything under `.cache/` (no global pollution)
   - Powers both gloat itself and downstream projects

4. **ingydotnet/gist** — Flagship downstream project
   - Demonstrates the full gloat toolchain in action
   - Uses Makes for dependency management
   - Publishes both source and compiled `go/` directory
   - Cross-platform binary releases via GitHub

### The Compilation Pipeline

The gloat compiler orchestrates a multi-stage transformation:

```
YAMLScript (.ys) → Clojure (.clj) → Glojure (.glj) → Go (.go) → Binary/WASM
```

Each stage is handled by a specialized tool:
- **YS → CLJ**: YAMLScript compiler (from yamlscript repo)
- **CLJ → GLJ**: Glojure's rewrite script (transforms Clojure idioms)
- **GLJ → GO**: Glojure compiler (generates Go loader files)
- **GO → BIN**: Go toolchain (builds native executables)

### How Makes Bootstraps Dependencies

Makes uses a 2-line Makefile preamble that clones itself into `.cache/makes/`:

```makefile
M := .cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)
```

All tool dependencies are then declared via `include` directives, and Makes
handles automatic installation on first use.
Users are prompted interactively before the first installation.

## Repository Layout

### Key Directories in gloat

```
gloat/
├── bin/
│   ├── gloat              # Bash wrapper (sets GLOAT_VERSION, PATH)
│   └── gloat.clj          # Main Babashka script (CLI logic)
├── template/
│   ├── clojure.clj        # Template for YS→CLJ conversion
│   ├── go.mod             # Template for generated Go modules
│   ├── main.go            # Template for binary entry point
│   ├── lib-main.go        # Template for shared library entry point
│   └── Makefile           # Template for generated Go directories
├── ys/                    # YS standard library (multi-stage build)
│   ├── src/               # Clojure source files
│   │   ├── yamlscript/    # Upstream YS code (util, common)
│   │   └── ys/            # Gloat-specific YS runtime (v0, std, fs, etc.)
│   ├── glj/               # Glojure intermediate files
│   ├── go/                # Compiled Go loader files
│   ├── pkg/               # Published Go module (sync from go/)
│   │   ├── go.mod         # ys/pkg Go module definition
│   │   └── all/           # Umbrella package for easy import
│   └── patch/             # Patches for babashka compatibility
├── common/
│   └── common.mk          # Version overrides (GLOJURE-VERSION, etc.)
├── util/                  # Helper scripts (getopt, etc.)
└── Makefile               # Build system (YS-PKG-VERSION, targets)
```

### The `ys/` Subtree: Multi-Stage Build

The YS standard library undergoes a three-stage build process:

1. **`ys/src/`** — Clojure source files
   - `yamlscript/` — Synced from upstream yamlscript repo (util, common)
   - `ys/` — Gloat-specific runtime (v0, std, fs, ipc, json, http, dwim)

2. **`ys/glj/`** — Glojure intermediate files
   - Generated via `make update` (runs rewrite.clj on each .clj file)

3. **`ys/go/`** — Compiled Go loader files
   - Generated via `make update` (compiles each .glj to Go)
   - Structure: `ys/go/namespace/path/loader.go`

4. **`ys/pkg/`** — Published Go module
   - Synced from `ys/go/` via `make ys-pkg` (excludes `all/`)
   - Tagged as a Go sub-module: `ys/pkg/v0.1.0`
   - Includes `all/all.go` umbrella package

### Templates and Placeholders

Gloat uses templates in `template/` with placeholder substitution:

**go.mod template** (`template/go.mod`):
```go
module GO-MODULE

go 1.24

require (
	github.com/gloathub/glojure GLOJURE-VERSION
	github.com/gloathub/gloat/ys/pkg YS-PKG-VERSION
)

replace github.com/gloathub/gloat/ys/pkg => GLOAT-ROOT/ys/pkg
```

Placeholders:
- `GO-MODULE` — Generated or user-specified module name
- `GLOJURE-VERSION` — From `common/common.mk` (e.g., `v0.6.5-rc4`)
- `YS-PKG-VERSION` — From `Makefile` (e.g., `v0.1.0`)
- `GLOAT-ROOT` — Absolute path to gloat repo
- `PACKAGE-PATH` — User namespace path (e.g., `foo/core`)
- `NAMESPACE` — User namespace symbol (e.g., `foo.core`)

The `replace` directive enables local development by pointing to the gloat
repo's `ys/pkg/` directory.
For published releases, this directive must be removed.

**main.go template** (`template/main.go`):
```go
package main

import (
	_ "GO-MODULE/pkg/PACKAGE-PATH"
	_ "github.com/gloathub/gloat/ys/pkg/all"
)
```

The umbrella import `ys/pkg/all` loads all YS stdlib packages in one line
(instead of 9 individual imports).

## The Makes Build System

### How It Works

Makes is a dependency management system built on GNU Make.
It provides a collection of `.mk` modules that handle tool installation,
version management, and common build tasks.

**Bootstrap Pattern** (2-line preamble in every Makefile):
```makefile
M := .cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)
```

This clones Makes into `.cache/makes/` if it doesn't exist.
All subsequent `include` directives load `.mk` modules from there.

**Key `.mk` Modules**:
- `init.mk` — Initializes Makes variables and paths
- `go.mk` — Installs and manages Go toolchain
- `glojure.mk` — Installs Glojure compiler (gloat fork with extended platform support)
- `babashka.mk` — Installs Babashka (runs gloat.clj)
- `yamlscript.mk` — Installs YAMLScript compiler
- `gloat.mk` — Provides `gloat-go`, `gloat-github-release` targets
- `gh.mk` — Installs GitHub CLI
- `clean.mk` — Provides clean/distclean targets
- `shell.mk` — Provides `make shell` for running commands with tools

All tools are installed under `.cache/.local/`:
```
.cache/
├── makes/                    # Makes repo (cloned)
│   ├── *.mk                  # Module definitions
│   └── share/                # Shared assets (configs, etc.)
└── .local/
    ├── bin/                  # Tool binaries (bb, glj, go, ys, etc.)
    ├── go/                   # GOPATH
    ├── cache/                # Build caches
    └── gloat-VERSION/        # Cloned gloat repo (per version)
```

### Using Makes

**Run commands with project dependencies**:
```bash
make shell cmd='gloat --version'
make shell cmd='go version'
make shell cmd='which bb'
```

**List installed dependencies**:
```bash
make path-deps
```

**Clean build artifacts**:
```bash
make clean       # Removes MAKES-CLEAN files
make distclean   # Also removes .cache/
```

### Version Management in Makes

Makes modules define version variables with `?=` (override-able defaults):

```makefile
# From go.mk
GO-VERSION ?= 1.25.6
```

Projects override these in their own Makefile or `common/common.mk` using `:=`:

```makefile
# From gloat's common/common.mk
GLOJURE-VERSION := 0.6.5-rc4
GLOJURE-COMMIT := gloat
GLOJURE-REPO := https://github.com/gloathub/glojure
```

The `:=` operator has higher precedence than `?=`, so project values win.

## How Gloat Generates Output

### The `gloat -o dir/` Flow

When you run `gloat foo.ys -o build/`, gloat performs these steps:

1. **Compile user code**: YS → CLJ → GLJ → Go
2. **Copy GLJ stdlib**: Copies `ys/glj/` tree to temp directory
3. **Compile all namespaces**: Runs `glj` to generate Go loader files
4. **Filter output**: Copies only user code from temp dir to `build/pkg/`
   - Excludes `yamlscript/` and `ys/` paths (stdlib)
   - Stdlib comes from `ys/pkg` module instead
5. **Render templates**: Generates `go.mod`, `main.go`, `Makefile`
6. **Populate go.mod**:
   - Module name (auto-detected or user-specified)
   - Glojure version (from `common/common.mk`)
   - YS pkg version (from `Makefile`)
   - Replace directive (for local development)

### The `ys/pkg` Go Module

The `ys/pkg` directory is a Go module that publishes the YS standard library:

**Structure**:
```
ys/pkg/
├── go.mod                              # Go module definition
├── yamlscript/common/loader.go         # Loader for yamlscript.common
├── yamlscript/util/loader.go           # Loader for yamlscript.util
├── ys/dwim/loader.go                   # Loader for ys.dwim
├── ys/fs/loader.go                     # Loader for ys.fs
├── ys/http/loader.go                   # Loader for ys.http
├── ys/ipc/loader.go                    # Loader for ys.ipc
├── ys/json/loader.go                   # Loader for ys.json
├── ys/std/loader.go                    # Loader for ys.std
├── ys/v0/loader.go                     # Loader for ys.v0
└── all/all.go                          # Umbrella package
```

**The Umbrella Import** (`ys/pkg/all/all.go`):
```go
package all

import (
	_ "github.com/gloathub/gloat/ys/pkg/yamlscript/common"
	_ "github.com/gloathub/gloat/ys/pkg/yamlscript/util"
	_ "github.com/gloathub/gloat/ys/pkg/ys/dwim"
	_ "github.com/gloathub/gloat/ys/pkg/ys/fs"
	_ "github.com/gloathub/gloat/ys/pkg/ys/http"
	_ "github.com/gloathub/gloat/ys/pkg/ys/ipc"
	_ "github.com/gloathub/gloat/ys/pkg/ys/json"
	_ "github.com/gloathub/gloat/ys/pkg/ys/std"
	_ "github.com/gloathub/gloat/ys/pkg/ys/v0"
)
```

This allows generated code to import the entire stdlib with a single line:
```go
import _ "github.com/gloathub/gloat/ys/pkg/all"
```

**The `replace` Directive** (local development vs published):

During development, the generated `go.mod` includes:
```go
replace github.com/gloathub/gloat/ys/pkg => /abs/path/to/gloat/ys/pkg
```

This lets you iterate on stdlib changes without publishing.
For releases, this directive is stripped (see "Downstream Project Setup" below).

### Binary/WASM/Lib Output Modes

When generating binaries (`-o foo`, `-o foo.wasm`, `-o foo.so`):

1. **Generate Go directory** in a temp location
2. **Run `go mod tidy`** to fetch dependencies
3. **Build with `go build`**:
   - Binary: `go build -ldflags "-s -w" -o foo main.go`
   - Library: `go build -buildmode=c-shared -o foo.so main.go`
   - WASM: `GOOS=wasip1 GOARCH=wasm go build -o foo.wasm main.go`
4. **Copy output** to user-specified location
5. **Clean up** temp directory

Cross-compilation uses `--platform=OS/ARCH` (e.g., `linux/amd64`, `darwin/arm64`).

## Downstream Project Setup

Downstream projects (like gist) use gloat via Makes to build and release
compiled binaries.

### Minimal Makefile

```makefile
# Example: gist/Makefile

M := .cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)

VERSION := 0.1.4
FILE := gist
GLOAT-RELEASE-WITH-GO-DIRECTORY := 1

include $M/init.mk
include $M/gloat.mk
```

**Key Variables**:
- `FILE` — The source file to compile (auto-detected if only one .ys/.clj)
  - Can omit extension (e.g., `FILE := gist` will auto-detect `gist.ys`)
- `VERSION` — The release version (used by `gloat-github-release`)
- `GLOAT-RELEASE-WITH-GO-DIRECTORY` — Set to `1` to include `go/` in releases (for `go install`)

### Platform Configuration

Create `.makes/gloat.config` to specify target platforms:

```ini
[gloat.platforms]
	name = linux/amd64
	name = linux/arm64
	name = darwin/amd64
	name = darwin/arm64
	name = windows/amd64
```

This configures which platforms are built during `make gloat-bin`.

### The `gloat-go` Target

The `gloat-go` target (from `gloat.mk`) generates the `go/` directory:

```bash
make gloat-go
```

**What it does**:
1. Runs `gloat FILE -o go/` to generate Go module
2. Rewrites module path from `github.com/gloathub/go` to the project's path
   (auto-detected from `git remote get-url origin`)
3. Moves `main.go` to `go/cmd/BINARY-NAME/main.go` (Go convention)
4. Updates `go/Makefile` with correct binary name
5. Runs `go mod tidy` in `go/`

**Key subtlety**: The current `gloat.mk` does NOT remove the `replace`
directive.
This is fine for local development, but for published releases, the `replace`
line must be stripped so `go install` fetches `ys/pkg` from the Go proxy.

### The `gloat-github-release` Target

The `gloat-github-release` target (from `gloat.mk`) automates the full release
process:

```bash
make gloat-github-release VERSION=0.1.5
```

**What it does**:
1. Updates `VERSION` in Makefile and source file
2. (If `GLOAT-RELEASE-WITH-GO-DIRECTORY` is set) Regenerates `go/` directory
3. Commits changes: `git commit -m 'Version v0.1.5'`
4. Tags release: `git tag v0.1.5`
5. (If `GLOAT-RELEASE-WITH-GO-DIRECTORY` is set) Tags Go submodule: `git tag go/v0.1.5`
6. Pushes code and tags: `git push && git push --tags`
7. Builds binaries for all platforms (via `gloat-bin`)
8. Creates GitHub release with binaries attached

### How `go install` Works

Once a project publishes its `go/` directory with the correct module path
(and no `replace` directive), users can install it via:

```bash
go install github.com/ingydotnet/gist/go/cmd/gist@v0.1.5
```

This works because:
1. The `go/` directory is committed to the repo
2. The Go submodule tag `go/v0.1.5` points to a commit with the correct `go/go.mod`
3. The `go.mod` requires `github.com/gloathub/gloat/ys/pkg` (fetched from proxy)
4. No `replace` directive interferes with module resolution

## Version Management

Gloat's version ecosystem has multiple moving parts.
All versions must be kept in sync for releases.

### All Version Locations

1. **`bin/gloat`** — Tool version (Bash wrapper)
   ```bash
   export GLOAT_VERSION=0.1.0
   ```

2. **`common/common.mk`** — Glojure fork version
   ```makefile
   GLOJURE-VERSION := 0.6.5-rc4
   GLOJURE-COMMIT := gloat
   GLOJURE-REPO := https://github.com/gloathub/glojure
   ```

3. **`Makefile`** — YS pkg Go module version
   ```makefile
   YS-PKG-VERSION ?= v0.1.0
   ```

4. **`ys/pkg/go.mod`** — Hardcoded Glojure dependency
   ```go
   require github.com/gloathub/glojure v0.6.5-rc4
   ```

5. **`Changes`** — Release changelog
   ```yaml
   - version: 0.1.0
     date:    Sun Feb  1 06:48:57 PM PST 2026
     changes:
     - Initial release
   ```

### How Versions Propagate

Gloat uses `get-make-var` in `bin/gloat.clj` to read Makefile variables:

```clojure
(def get-make-var
  (memoize
   (fn [var-name]
     (let [result (process/shell
                   {:out :string}
                   "make" "--no-print-directory"
                   (str "--eval=print-" var-name ": ; @echo $(" var-name ")")
                   (str "print-" var-name))]
       (str/trim (:out result))))))
```

This is called during template rendering:

```clojure
(let [glojure-version (get-make-var "GLOJURE-VERSION")
      ys-pkg-version (get-make-var "YS-PKG-VERSION")
      result (render-template
              template-content
              [["GLOJURE-VERSION" glojure-version]
               ["YS-PKG-VERSION" ys-pkg-version]])]
  ...)
```

So:
- `common/common.mk` → `GLOJURE-VERSION` → `go.mod` template
- `Makefile` → `YS-PKG-VERSION` → `go.mod` template

### Go Module Tagging

Go uses version tags to identify module versions:

**Main module** (e.g., gloat itself):
```bash
git tag v0.1.0
```

**Sub-module** (e.g., `ys/pkg`):
```bash
git tag ys/pkg/v0.1.0
```

**Go sub-directory** (e.g., `go/` in downstream projects):
```bash
git tag go/v0.1.5
```

The tag path corresponds to the module's location in the repo.

## The YS Standard Library

### Build Pipeline

The YS stdlib is built in multiple stages:

1. **`ys/src/*.clj`** — Clojure source files
2. **`ys/glj/*.glj`** — Glojure intermediate files (via `rewrite.clj`)
3. **`ys/go/*/loader.go`** — Go loader files (via `glj` compiler)
4. **`ys/pkg/`** — Published Go module (synced from `ys/go/`)

### Build Targets

**`make update`** — Rebuild all Go files from source:
```bash
make update
```
This runs the full pipeline: CLJ → GLJ → GO for all stdlib namespaces.

**`make ys-pkg`** — Sync `ys/go/` to `ys/pkg/` and tidy:
```bash
make ys-pkg
```
This copies Go files to `ys/pkg/`, excluding `all/` and go.mod/go.sum, then
runs `go mod tidy`.

**`make tag-ys-pkg`** — Create version tag:
```bash
make tag-ys-pkg YS-PKG-VERSION=0.1.0
```
This creates an annotated tag `ys/pkg/v0.1.0`.

### The `ys/pkg/all/all.go` Umbrella Package

The `all` package provides a convenience import for all stdlib packages:

```go
package all

import (
	_ "github.com/gloathub/gloat/ys/pkg/yamlscript/common"
	_ "github.com/gloathub/gloat/ys/pkg/yamlscript/util"
	_ "github.com/gloathub/gloat/ys/pkg/ys/dwim"
	_ "github.com/gloathub/gloat/ys/pkg/ys/fs"
	_ "github.com/gloathub/gloat/ys/pkg/ys/http"
	_ "github.com/gloathub/gloat/ys/pkg/ys/ipc"
	_ "github.com/gloathub/gloat/ys/pkg/ys/json"
	_ "github.com/gloathub/gloat/ys/pkg/ys/std"
	_ "github.com/gloathub/gloat/ys/pkg/ys/v0"
)
```

This is manually maintained (not auto-generated).
It's excluded from the `make ys-pkg` sync to avoid overwriting it.

### Upstream Sync

Most YS stdlib files come from the upstream `yaml/yamlscript` repo:
- `yamlscript/util.clj`
- `yamlscript/common.clj`

These are synced via:
```bash
make ys/src/yamlscript/util.clj    # Updates from upstream
```

Gloat-specific files (no upstream equivalents):
- `ys/src/ys/v0.clj` — Main YS runtime
- `ys/src/ys/fs.clj` — Go filesystem interop (not babashka.fs)
- `ys/src/ys/ipc.clj` — Go IPC interop (not babashka.process)
- `ys/src/ys/json.clj` — Pure Clojure JSON (not clojure.data.json)
- `ys/src/ys/http.clj` — Go net/http wrapper (not babashka.http-client)

## The Glojure Fork

### Why Fork?

The gloat project maintains a fork of glojurelang/glojure for several reasons:

1. **Go compilation improvements** — Patches to improve generated Go code
2. **Extended platform support** — Adds 15-20 additional cross-compilation targets
3. **Bug fixes** — Fixes for issues like `seqable?` handling in generated code
4. **Stability** — Pins a known-good version with gloat-specific features

### Fork Details

**Repository**: `gloathub/glojure`
**Branch**: `gloat`
**Configured in**: `common/common.mk`

```makefile
GLOJURE-VERSION := 0.6.5-rc4
GLOJURE-COMMIT := gloat
GLOJURE-REPO := https://github.com/gloathub/glojure
GLOJURE-GET-URL := github.com/gloathub/glojure/cmd/glj
```

### Upstream Sync Aspirations

The long-term goal is to upstream all patches to glojurelang/glojure and
eliminate the fork.
This requires:
1. Extracting patch set from `gloat` branch
2. Submitting PRs to upstream
3. Waiting for upstream releases
4. Switching gloat to use upstream versions

## Development Workflow

### Initial Setup

**Add gloat to PATH**:
```bash
cd /path/to/gloat
source .rc
```

The `.rc` file adds `bin/` to your PATH.

**First run installs dependencies**:
```bash
gloat --help
```

This prompts you to install dependencies (bb, glj, go, ys) into `.cache/.local/`.
After installation, all tools are available via the PATH from `make path`.

### Running Tests

**Run gloat test suite**:
```bash
make test           # Run prove tests
make test v=1       # Verbose test output
```

**Run tests in Docker** (clean environment):
```bash
make distclean      # Must clean first
make test-docker
```

### Modifying the Standard Library

**Edit a stdlib file**:
```bash
vim ys/src/ys/std.clj
```

**Rebuild Go files**:
```bash
make update         # Regenerates ys/glj/ and ys/go/
```

**Sync to published module**:
```bash
make ys-pkg         # Copies to ys/pkg/ and runs go mod tidy
```

**Test your changes**:
```bash
make test
```

**Commit the changes**:
```bash
git add ys/src/ys/std.clj ys/glj/ys/std.glj ys/go/ys/std/loader.go
git commit -m "Improve ys.std documentation"
```

### Typical Development Cycle

1. **Make changes** to gloat source (`bin/gloat.clj`) or stdlib (`ys/src/`)
2. **Run tests** to verify: `make test`
3. **Test with example** project: `make run FILE=demo/foo.ys`
4. **Update stdlib** if needed: `make update && make ys-pkg`
5. **Commit changes**

### Working with Downstream Projects

**Test gloat changes with a downstream project**:

```bash
cd /path/to/gist
rm -rf .cache/makes     # Clear Makes cache
cd .cache
ln -s /path/to/gloat gloat-main   # Use local gloat
cd ..
make gloat-go           # Regenerate go/ with local gloat
```

This bypasses the git clone step in `gloat.mk` and uses your local gloat repo.

---

This tutorial should provide a comprehensive understanding of how the gloat
ecosystem works.
For more specific information about release management, see `doc/release-plan.md`.

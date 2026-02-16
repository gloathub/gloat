# Releasing Glojure (gloathub/glojure Fork)

This document provides step-by-step instructions for releasing the
`gloathub/glojure` fork, which is a patched version of the upstream
`glojurelang/glojure` project.

## Overview

The `gloathub/glojure` fork includes:
- Patches for improved Go code generation
- Extended cross-compilation support (15-20 additional platforms)
- Bug fixes (e.g., `seqable?` handling)
- Changes required by gloat

Releases involve building multi-platform binaries and tagging a version that
gloat can depend on.

## Repository Structure

**Fork**: `gloathub/glojure`
**Upstream**: `glojurelang/glojure`
**Branch**: `gloat` (main development branch for gloat-specific changes)

**Git remotes**:
```bash
origin      git@github.com:glojurelang/glojure (upstream)
gloathub    git@github.com:gloathub/glojure (fork)
```

## Prerequisites

- [ ] All changes have been tested
- [ ] Tests pass: `make test`
- [ ] Access to push to `gloathub/glojure`
- [ ] Go 1.24+ installed (or will be auto-installed by Makefile)

## Understanding the Build Process

The glojure build has several stages:

### 1. **Update Clojure Sources** (Optional)

Fetches the latest Clojure standard library from upstream:

```bash
make update-clojure-sources CLOJURE-VERSION=clojure-1.12.1
```

This downloads `.clj` files to `scripts/rewrite-core/originals/`.

### 2. **Stdlib Targets**

Rewrites Clojure stdlib files to Glojure (`.clj` → `.glj`):

```bash
make stdlib-targets
```

Converts files in `scripts/rewrite-core/originals/*.clj` to
`pkg/stdlib/clojure/*.glj`.

### 3. **Generate**

Runs `go generate ./...` to generate any Go code:

```bash
make generate
```

### 4. **AOT (Ahead-of-Time Compilation)**

Pre-compiles core Clojure namespaces to Go code for faster startup:

```bash
make aot
```

**What this does**:
- Compiles these namespaces to Go:
  - `clojure.core`
  - `clojure.core.async`
  - `clojure.string`
  - `clojure.template`
  - `clojure.test`
  - `clojure.uuid`
  - `clojure.walk`
  - `glojure.go.io`
  - `glojure.go.types`
- Stores compiled Go code in `pkg/stdlib/` (embedded in the glj binary)
- Significantly improves glj startup time and compilation speed

**When to run `make aot`**:
- After updating Clojure sources
- After modifying core library code
- Before releasing a new version
- When `make all` fails due to AOT issues

### 5. **GLJ Imports**

Generates platform-specific import files:

```bash
make glj-imports
```

Creates `pkg/gen/gljimports/gljimports_*.go` for each platform.

### 6. **GLJ Binaries**

Builds the `glj` compiler for all platforms:

```bash
make glj-bins
```

Builds binaries for all platforms in `GO-PLATFORMS`:
- `bin/linux_amd64/glj`
- `bin/linux_arm64/glj`
- `bin/darwin_amd64/glj`
- `bin/darwin_arm64/glj`
- `bin/windows_amd64/glj.exe`
- `bin/js_wasm/glj.wasm`
- `bin/wasip1_wasm/glj.wasm`
- Plus many more platforms

### Full Build

Run everything:

```bash
make all
```

This runs: `update-clojure-sources` (if `force=1`), `stdlib-targets`,
`generate`, `aot`, `glj-imports`, `glj-bins`.

## Release Process

### Step 1: Ensure Clean State

Make sure you're on the `gloat` branch with a clean working directory:

```bash
cd /path/to/glojure
git checkout gloat
git status    # Should be clean
```

**Verify**:
- [ ] On `gloat` branch
- [ ] No uncommitted changes

### Step 2: Sync with Upstream (Optional)

If you want to incorporate upstream changes:

```bash
# Fetch upstream changes
git fetch origin main

# Merge or rebase as appropriate
git merge origin/main
# OR
git rebase origin/main

# Resolve any conflicts
# Test thoroughly after merging
```

**Skip this if**:
- You're only releasing existing changes
- Upstream hasn't changed
- You want to release current state as-is

### Step 3: Run Full Build

Clean and rebuild everything:

```bash
make clean
make all
```

**This will**:
1. Update clojure sources (if `force=1`)
2. Rewrite stdlib (`.clj` → `.glj`)
3. Run `go generate`
4. Run AOT compilation
5. Generate platform-specific imports
6. Build binaries for all platforms

**Note**: First build may take 10-20 minutes.
Subsequent builds are faster.

**Verify**:
- [ ] Build completes without errors
- [ ] Binaries exist in `bin/*/glj` (check a few platforms)

### Step 4: Run Tests

Run the test suite:

```bash
make test
```

**This runs**:
- `make test-glj` — Tests in `test/glojure/*.glj`
- `make test-suite` — Clojure test suite (if available)

**Verify**:
- [ ] All tests pass (or expected failures match expectations)
- [ ] No new test failures

### Step 5: Commit Changes (If Any)

If you made code changes, commit them:

```bash
git add -A
git status
git commit -m "Description of changes"
```

### Step 6: Tag the Release

Choose a version number.
The fork uses the same versioning scheme as upstream, with optional suffixes:

**Version scheme**:
- `v0.6.5` — Matches upstream release
- `v0.6.5-rc1` — Release candidate
- `v0.6.5-rc2`, `v0.6.5-rc3`, etc. — Subsequent RCs
- `v0.6.5.1`, `v0.6.5.2` — Patch releases on top of upstream version

Check the latest tag:

```bash
git tag | tail -5
```

Create an annotated tag:

```bash
git tag -a v0.6.5-rc5 -m "Release v0.6.5-rc5

Glojure fork for gloat with the following changes:
- Description of patches
- Extended cross-compilation support (15-20 additional platforms)
- Bug fixes
"
```

### Step 7: Push to Fork

Push the `gloat` branch and tags to the fork:

```bash
git push gloathub gloat
git push gloathub v0.6.5-rc5
```

**Verify**:
- [ ] Code is on GitHub: https://github.com/gloathub/glojure/tree/gloat
- [ ] Tag is visible: https://github.com/gloathub/glojure/tags

### Step 8: Verify Go Module Proxy

Wait a few minutes, then verify the module is fetchable:

```bash
GOPROXY=proxy.golang.org go list -m github.com/gloathub/glojure@v0.6.5-rc5
```

**Expected output**:
```
github.com/gloathub/glojure v0.6.5-rc5
```

**Verify**:
- [ ] Module is fetchable from Go proxy
- [ ] Version matches expected

### Step 9: Update Gloat to Use New Version

Update gloat's `common/common.mk` to reference the new glojure version:

```bash
cd /path/to/gloat
vim common/common.mk
```

Update:
```makefile
GLOJURE-VERSION := v0.6.5-rc5
GLOJURE-COMMIT := gloat
GLOJURE-REPO := https://github.com/gloathub/glojure
```

Also update `ys/pkg/go.mod`:
```bash
vim ys/pkg/go.mod
```

Update the `require` line:
```go
require github.com/gloathub/glojure v0.6.5-rc5
```

Test with gloat:
```bash
cd /path/to/gloat
make clean
make update
make ys-pkg
make test
```

**Verify**:
- [ ] Gloat builds successfully with new glojure version
- [ ] All gloat tests pass
- [ ] Generated code works correctly

If tests pass, commit the gloat changes:
```bash
git add common/common.mk ys/pkg/go.mod
git commit -m "Update to glojure v0.6.5-rc5"
```

## Troubleshooting

### `make aot` fails

**Cause**: Core library changes or missing dependencies.

**Solution**:
1. Check that stdlib-targets succeeded: `make stdlib-targets`
2. Run with verbose output: `make aot force=1`
3. Check for errors in generated `.glj` files
4. Verify Go version: `go version` (needs 1.24+)

### Binaries fail to build for some platforms

**Cause**: Platform-specific issues or missing platform support.

**Solution**:
1. Build just one platform: `make bin/linux_amd64/glj`
2. Check Go platform support: `go tool dist list`
3. Look for error messages in build output
4. Some platforms may not be fully supported yet

### WASM binaries fail

**Cause**: WASM targets can have platform-specific issues.

**Solution**:
1. Build just WASM: `make bin/wasip1_wasm/glj.wasm`
2. Check that Go supports WASM: `go env GOOS GOARCH`
3. Verify WASM-specific code compiles
4. Test with wasmtime: `wasmtime bin/wasip1_wasm/glj.wasm`

### Tests fail after merge from upstream

**Cause**: Upstream changes may conflict with fork patches.

**Solution**:
1. Review test failures: `make test-glj`
2. Check if patches need updating
3. Verify stdlib rewrites still work: `make stdlib-targets force=1`
4. May need to rebase or adjust patches

## Understanding the Fork

### Why Fork?

The fork exists because:
1. **Gloat-specific patches** — Changes needed for gloat's use case
2. **Faster iteration** — Can release without waiting for upstream
3. **Extended platform support** — Added 15-20 additional cross-compilation targets
4. **Bug fixes** — Fixes not yet in upstream

### Upstreaming Changes

Long-term goal: Get patches into upstream and eliminate the fork.

**To upstream a change**:
1. Create a clean patch from the `gloat` branch
2. Submit PR to `glojurelang/glojure`
3. Wait for review and merge
4. Once merged, update fork to use upstream version
5. Remove patch from `gloat` branch

### Maintaining the Fork

**Regular maintenance**:
- Sync with upstream periodically
- Keep patches minimal and well-documented
- Test thoroughly before releasing
- Coordinate with gloat releases

## Rollback Plan

If a release has issues:

```bash
cd /path/to/glojure

# Delete local tag
git tag -d v0.6.5-rc5

# Delete remote tag
git push gloathub :v0.6.5-rc5

# Revert commits if needed
git reset --hard HEAD~1
git push gloathub gloat --force
```

**Important**: Go proxy caches are immutable.
Once published, you can't fully delete it.
Release a new version with fixes instead.

## Summary Checklist

- [ ] On `gloat` branch with clean working directory
- [ ] Sync with upstream (if desired)
- [ ] Run `make clean && make all`
- [ ] Run `make test`
- [ ] Commit any code changes
- [ ] Tag release: `git tag -a vX.Y.Z-rcN -m "..."`
- [ ] Push: `git push gloathub gloat && git push gloathub vX.Y.Z-rcN`
- [ ] Verify on Go proxy: `go list -m github.com/gloathub/glojure@vX.Y.Z-rcN`
- [ ] Update gloat's `common/common.mk` and `ys/pkg/go.mod`
- [ ] Test gloat with new version: `make test`
- [ ] Commit gloat changes

---

## Quick Reference

**Full build**:
```bash
make clean
make all
make test
```

**AOT only** (after changing core libs):
```bash
make aot
```

**Single platform** (for testing):
```bash
make bin/linux_amd64/glj
```

**Force rebuild** (if incremental build is broken):
```bash
make all force=1
```

**Update from specific Clojure version**:
```bash
make all force=1 CLOJURE-VERSION=clojure-1.12.1
```

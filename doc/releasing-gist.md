# Releasing Gist (and Other Downstream Projects)

This document provides step-by-step instructions for releasing downstream
projects that use gloat, such as gist.

These instructions apply to any project using `gloat.mk` from makeplus/makes
with `GLOAT-RELEASE-WITH-GO-DIRECTORY` enabled.

## Overview

A downstream project release involves:
1. Updating to the desired gloat version
2. Regenerating the `go/` directory
3. Creating a release with cross-platform binaries
4. Publishing Go submodule tags (for `go install` support)

The `gloat-github-release` target from `gloat.mk` automates most of this
process.

## Prerequisites

- [ ] Gloat has been released and `ys/pkg` is available on Go proxy
- [ ] All project changes have been tested
- [ ] Access to push tags to the project repository
- [ ] GitHub CLI (`gh`) is authenticated
- [ ] `.makes/gloat.config` exists with desired platform list (or use defaults)

## Step 1: Update Gloat Version

### 1.1: Update Makes Cache

Refresh the Makes cache to get the latest `gloat.mk`:

```bash
cd /path/to/your-project
cd .cache/makes
git pull origin main
cd ../..
```

**Verify**:
- [ ] Latest `gloat.mk` is available in `.cache/makes/`

### 1.2: Pin Gloat Version

Edit your project's `Makefile` to pin the gloat version.

Add or update this line **before** the `include` directives:

```makefile
GLOAT-VERSION := v0.2.0
```

**Example Makefile**:
```makefile
M := .cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)

GLOAT-VERSION := v0.2.0

VERSION := 0.1.5
FILE := gist
GLOAT-RELEASE-WITH-GO-DIRECTORY := 1

include $M/init.mk
include $M/gloat.mk
```

**Notes**:
- Use a tagged version (e.g., `v0.2.0`) for reproducibility
- You can use `main` for the latest development version
- The variable must be set before `include $M/gloat.mk`

### 1.3: Test Locally

Before releasing, test that the gloat version works:

```bash
# Clean old gloat install
rm -rf .cache/.local/gloat-*

# Test compilation
make gloat-go

# Verify generated go/ directory
cat go/go.mod
```

**Verify**:
- [ ] `go/` directory generates successfully
- [ ] `go/go.mod` has correct gloat and ys/pkg versions
- [ ] No `replace` directive for `ys/pkg` (should fetch from Go proxy)

## Step 2: Run the Release Process

### 2.1: Decide on Version Number

Choose a version number following semantic versioning:
- **Patch** (0.1.4 → 0.1.5): Bug fixes, no new features
- **Minor** (0.1.5 → 0.2.0): New features, backwards compatible
- **Major** (0.2.0 → 1.0.0): Breaking changes

### 2.2: Run Release Target

Run the automated release process:

```bash
make gloat-github-release VERSION=0.1.5
```

**This will**:
1. Update `VERSION` in `Makefile`
2. Update version in source file (e.g., `gist.ys`)
3. Regenerate `go/` directory via `make gloat-go`
4. Commit changes: `git commit -m 'Version v0.1.5'`
5. Tag release: `git tag v0.1.5`
6. Tag Go submodule: `git tag go/v0.1.5`
7. Push code and tags to GitHub
8. Build binaries for all configured platforms
9. Create GitHub release with binaries attached

**Note**: This process will prompt for confirmation before pushing to GitHub.

## Step 3: Verify the Release

### 3.1: Verify GitHub Release

Check that the release was created:

```bash
# Open in browser
gh release view v0.1.5 --web
```

**Verify**:
- [ ] Release exists at https://github.com/USER/PROJECT/releases/tag/v0.1.5
- [ ] Binaries are attached for all platforms (linux-amd64, darwin-arm64, etc.)
- [ ] Release notes are present

### 3.2: Verify Go Directory

Inspect the committed `go/` directory:

```bash
ls -la go/

# Expected structure:
# go/
# ├── cmd/PROJECT/main.go    (moved from go/main.go)
# ├── pkg/NAMESPACE/         (user code only)
# │   └── loader.go
# ├── go.mod
# ├── go.sum
# └── Makefile
```

**Verify `go/go.mod`**:
```bash
cat go/go.mod
```

Should contain:
- [ ] Correct module path (e.g., `github.com/ingydotnet/gist/go`)
- [ ] `require github.com/gloathub/gloat/ys/pkg vX.Y.Z`
- [ ] **NO `replace` directive** for `ys/pkg` (critical for `go install`)
- [ ] `require github.com/gloathub/glojure vX.Y.Z`

**Verify `go/cmd/PROJECT/main.go`**:
```bash
head -15 go/cmd/*/main.go
```

Should import:
- [ ] User package: `_ "github.com/USER/PROJECT/go/pkg/..."`
- [ ] YS stdlib: `_ "github.com/gloathub/gloat/ys/pkg/all"`
- [ ] **NO individual stdlib imports** (yamlscript.util, ys.std, etc.)

**Verify `go/pkg/`**:
```bash
find go/pkg -name "*.go"
```

Should contain:
- [ ] Only user code (your project's namespaces)
- [ ] **NO `yamlscript/`** directories (stdlib comes from `ys/pkg`)
- [ ] **NO `ys/`** directories (stdlib comes from `ys/pkg`)

### 3.3: Test `go install`

Verify that users can install via `go install`:

```bash
# Install from a clean directory
cd /tmp
go install github.com/USER/PROJECT/go/cmd/PROJECT@v0.1.5

# Verify it works
~/go/bin/PROJECT --version
```

**Expected**: Should show the correct version.

**If this fails**, check:
- Is the `go/v0.1.5` tag pushed?
- Does `go/go.mod` have the correct module path?
- Is there a `replace` directive (there shouldn't be)?
- Is `ys/pkg` fetchable from Go proxy?

### 3.4: Test Downloaded Binaries

Download and test binaries from the GitHub release:

```bash
# Example: Linux amd64
wget https://github.com/USER/PROJECT/releases/download/v0.1.5/PROJECT-linux-amd64
chmod +x PROJECT-linux-amd64
./PROJECT-linux-amd64 --version
```

**Verify**:
- [ ] Binaries run on their target platforms
- [ ] `--version` shows correct version
- [ ] No runtime errors

## Step 4: Fresh Install Test (Optional)

Test in a completely fresh environment:

```bash
# In Docker or a fresh VM
go install github.com/USER/PROJECT/go/cmd/PROJECT@v0.1.5
~/go/bin/PROJECT --help
```

**Verify**:
- [ ] Installation succeeds
- [ ] Binary runs without errors
- [ ] `ys/pkg` is fetched from Go proxy (check `~/go/pkg/mod/`)

## Rollback Plan

If something goes wrong:

```bash
cd /path/to/your-project

# Undo version commit
git reset --hard HEAD~1

# Delete local tags
git tag -d v0.1.5
git tag -d go/v0.1.5

# Delete remote tags
git push origin :v0.1.5
git push origin :go/v0.1.5

# Delete GitHub release
gh release delete v0.1.5
```

**Important**: Go proxy caches are immutable.
Once a module is published, you can't fully delete it.
You can only deprecate it or publish a newer version.

## Customizing the Release Process

### Platform Configuration

Edit `.makes/gloat.config` to specify which platforms to build:

```ini
[gloat.platforms]
	name = linux/amd64
	name = linux/arm64
	name = darwin/amd64
	name = darwin/arm64
	name = windows/amd64
```

### Makefile Variables

**`GLOAT-VERSION`** — Gloat version to use:
```makefile
GLOAT-VERSION := v0.2.0    # Use specific version
GLOAT-VERSION := main      # Use latest development
```

**`GLOAT-RELEASE-WITH-GO-DIRECTORY`** — Include `go/` in releases:
```makefile
GLOAT-RELEASE-WITH-GO-DIRECTORY := 1
```

This regenerates `go/` during releases and creates the `go/vX.Y.Z` tag.

**`FILE`** — Source file to compile:
```makefile
FILE := myapp              # Auto-detects myapp.ys or myapp.clj
FILE := myapp.ys           # Explicit extension
```

**`VERSION`** — Project version (updated by release process):
```makefile
VERSION := 0.1.5
```

## Troubleshooting

### `go install` fails with "unknown revision"

**Cause**: The `go/vX.Y.Z` tag wasn't pushed or Go proxy hasn't indexed it yet.

**Solution**:
1. Verify tag exists: `git tag | grep go/`
2. Push tags: `git push origin --tags`
3. Wait 5 minutes for Go proxy to index

### Generated `go/` has `replace` directive

**Cause**: Old version of `gloat.mk` doesn't remove the `replace` directive.

**Solution**:
1. Update Makes cache: `cd .cache/makes && git pull`
2. Regenerate: `rm -rf go/ && make gloat-go`
3. Verify: `grep replace go/go.mod` (should be empty)

### Binaries fail to build for some platforms

**Cause**: Platform not supported or cross-compilation issues.

**Solution**:
1. Check `.makes/gloat.config` for typos
2. Verify platform is supported by Go: `go tool dist list`
3. Check gloat issues: https://github.com/gloathub/gloat/issues

---

## Summary Checklist

- [ ] Update Makes cache: `cd .cache/makes && git pull`
- [ ] Pin gloat version in Makefile
- [ ] Test locally: `make gloat-go`
- [ ] Run release: `make gloat-github-release VERSION=X.Y.Z`
- [ ] Verify GitHub release has binaries
- [ ] Verify `go/go.mod` has no `replace` directive
- [ ] Test `go install github.com/USER/PROJECT/go/cmd/PROJECT@vX.Y.Z`
- [ ] Test downloaded binaries
- [ ] Optional: Fresh install test in clean environment

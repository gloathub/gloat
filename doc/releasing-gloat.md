# Releasing Gloat

This document provides step-by-step instructions for publishing a gloat release.

## Overview

A gloat release involves:
1. Publishing the `ys/pkg` Go module (YS standard library)
2. Updating `gloat.mk` in makeplus/makes (if needed)
3. Tagging and releasing gloat itself

The `ys/pkg` module must be published first since gloat's generated code depends
on it.

## Prerequisites

- [ ] All changes have been tested and verified
- [ ] All tests pass: `make test`
- [ ] The `ys/pkg/` directory is up to date: `make ys-pkg`
- [ ] Access to push tags to `gloathub/gloat`
- [ ] Access to push to `makeplus/makes` (if updating `gloat.mk`)
- [ ] GitHub CLI (`gh`) is authenticated (for creating releases)

## Step 1: Prepare the Repository

### 1.1: Ensure Clean Working Directory

Ensure you're on the `main` branch with all changes committed:

```bash
cd /path/to/gloat
git checkout main
git status    # Should show clean working directory
```

**Verify**:
- [ ] On `main` branch with clean working directory
- [ ] `ys/pkg/` directory exists with all loader files
- [ ] `ys/pkg/go.mod` has the correct `require` line

### 1.2: Rebuild Standard Library

Ensure all stdlib files are up to date:

```bash
make update      # Rebuilds ys/glj/ and ys/go/ from ys/src/
make ys-pkg      # Syncs ys/go/ to ys/pkg/ and runs go mod tidy
```

**Verify**:
- [ ] `ys/pkg/` contains all expected loader files
- [ ] `ys/pkg/all/all.go` exists and imports all stdlib packages
- [ ] `go mod tidy` in `ys/pkg/` succeeds with no errors

### 1.3: Run Tests

Run the full test suite to ensure everything works:

```bash
make test        # Run all tests
```

**Verify**:
- [ ] All tests pass
- [ ] No compilation errors

### 1.4: Update Version Numbers

Check current version numbers:

```bash
grep GLOAT_VERSION bin/gloat
grep "^- version:" Changes
grep "YS-PKG-VERSION" Makefile
```

**Version locations**:
- `GLOAT_VERSION` in `bin/gloat` — Tool version
- `YS-PKG-VERSION` in `Makefile` — YS pkg Go module version
- `version:` in `Changes` — Changelog entry

If bumping versions, update all three locations consistently.

**Example for version 0.2.0**:

**`bin/gloat`**:
```bash
export GLOAT_VERSION=0.2.0
```

**`Makefile`**:
```makefile
YS-PKG-VERSION ?= v0.2.0
```

**`Changes`** (add new entry at top):
```yaml
- version: 0.2.0
  date:    YYYY-MM-DD
  changes:
  - Description of changes
```

**Verify**:
- [ ] `bin/gloat` has correct `GLOAT_VERSION`
- [ ] `Makefile` has correct `YS-PKG-VERSION`
- [ ] `Changes` file has entry for the version with today's date

### 1.5: Commit Version Changes

Commit any version updates:

```bash
git add bin/gloat Makefile Changes
git status      # Review what's being committed
git commit -m "Bump version to v0.2.0"
```

**Verify**:
- [ ] Working directory is clean: `git status`

## Step 2: Publish the `ys/pkg` Go Module

The `ys/pkg` directory is a Go sub-module that must be tagged separately.

### 2.1: Tag the `ys/pkg` Module

Create an annotated tag for the `ys/pkg` module:

```bash
make tag-ys-pkg
```

This runs: `git tag -a ys/pkg/v0.2.0 -m "Release ys/pkg v0.2.0"`

**Verify**:
- [ ] Tag exists: `git tag | grep ys/pkg`

### 2.2: Push Code and Tags

Push the main branch and all tags:

```bash
git push origin main
git push origin --tags
```

**Verify**:
- [ ] Code is on GitHub: https://github.com/gloathub/gloat
- [ ] Tags are visible: https://github.com/gloathub/gloat/tags
- [ ] `ys/pkg/vX.Y.Z` tag points to correct commit

### 2.3: Verify Go Module Proxy

Wait a few minutes for the Go module proxy to index the new module, then verify:

```bash
GOPROXY=proxy.golang.org go list -m github.com/gloathub/gloat/ys/pkg@v0.2.0
```

**Expected output**:
```
github.com/gloathub/gloat/ys/pkg v0.2.0
```

If this fails with "unknown revision", wait a few more minutes and retry.
The proxy can take up to 5 minutes to index new modules.

**Verify**:
- [ ] Module is fetchable from Go proxy
- [ ] Version matches expected

## Step 3: Update `gloat.mk` in makeplus/makes (If Needed)

**Skip this step if `gloat.mk` doesn't need changes.**

The `gloat.mk` file in makeplus/makes provides the `gloat-go` and
`gloat-github-release` targets for downstream projects.

### 3.1: Clone makeplus/makes

```bash
cd /tmp
git clone https://github.com/makeplus/makes
cd makes
```

### 3.2: Make Necessary Changes

Edit `gloat.mk` as needed.
Common changes:
- Updating default `GLOAT-VERSION`
- Modifying `gloat-go` target behavior
- Adding new configuration options

**Example**: The initial `ys/pkg` release required adding a line to remove the
`replace` directive from generated `go.mod` files:

```makefile
$Q perl -ni -e \
    'print unless /^replace github\.com\/gloathub\/gloat\/ys\/pkg =>/' \
    $@/go.mod
```

### 3.3: Commit and Push

```bash
git add gloat.mk
git commit -m "Your descriptive commit message"
git push origin main
```

**Verify**:
- [ ] Changes are on GitHub: https://github.com/makeplus/makes/blob/main/gloat.mk

## Step 4: Tag Gloat Release

Now that the `ys/pkg` module is published (and Makes is updated if needed), tag
the gloat release itself.

### 4.1: Tag Gloat Version

Create an annotated tag with release notes:

```bash
cd /path/to/gloat
git tag -a v0.2.0 -m "Release v0.2.0

Description of the release.

Changes:
- Feature 1
- Feature 2
- Bug fix 1
"
```

### 4.2: Push Tag

```bash
git push origin v0.2.0
```

**Verify**:
- [ ] Tag is on GitHub: https://github.com/gloathub/gloat/tags

### 4.3: Create GitHub Release (Optional)

You can create a GitHub release manually via the web UI or using `gh`:

```bash
gh release create v0.2.0 \
  --title "gloat v0.2.0" \
  --notes "Release notes here..."
```

**Verify**:
- [ ] Release appears at https://github.com/gloathub/gloat/releases

## Rollback Plan

If something goes wrong, you can roll back:

### Roll Back Gloat Tags

```bash
cd /path/to/gloat
git tag -d v0.2.0                    # Delete local tag
git tag -d ys/pkg/v0.2.0             # Delete local ys/pkg tag
git push origin :v0.2.0              # Delete remote tag
git push origin :ys/pkg/v0.2.0       # Delete remote ys/pkg tag
gh release delete v0.2.0             # Delete GitHub release
```

### Roll Back Makes Changes

```bash
cd /path/to/makes
git revert HEAD                      # Revert last commit
git push origin main
```

**Important**: Once modules are published to the Go proxy, they cannot be fully
deleted (Go proxy caches are immutable).
You can only deprecate them or publish a newer version.

## Post-Release

After releasing gloat:
1. Update downstream projects (e.g., gist) to use the new version
2. Announce the release (if appropriate)
3. Update any documentation that references version numbers

See `doc/releasing-gist.md` for instructions on releasing downstream projects.

---

## Summary Checklist

- [ ] Clean working directory on `main` branch
- [ ] Run `make update && make ys-pkg`
- [ ] Run `make test`
- [ ] Update version numbers in `bin/gloat`, `Makefile`, `Changes`
- [ ] Commit version changes
- [ ] Run `make tag-ys-pkg`
- [ ] Push code and tags: `git push origin main --tags`
- [ ] Verify `ys/pkg` on Go proxy
- [ ] Update `gloat.mk` in makeplus/makes (if needed)
- [ ] Create `vX.Y.Z` tag for gloat
- [ ] Push gloat tag
- [ ] Create GitHub release (optional)

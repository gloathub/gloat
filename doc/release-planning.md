# Release Planning: The Gloat Ecosystem

This document provides an overview of how releases work across the gloat
ecosystem and guides you through planning a coordinated release.

## The Four Release Documents

The gloat ecosystem has four interconnected projects, each with its own release
process:

1. **`releasing-glojure.md`** — Releasing the Glojure compiler fork
2. **`releasing-gloat.md`** — Releasing gloat (the AOT tool)
3. **`releasing-gist.md`** — Releasing downstream projects (gist, etc.)
4. **This document** — Planning releases across the ecosystem

## Dependency Flow

Understanding the dependency chain is critical for release planning:

```
glojurelang/glojure (upstream)
    ↓ (fork + patches)
gloathub/glojure (fork)
    ↓ (dependency)
gloathub/gloat (compiler)
    ├── ys/pkg (stdlib Go module)
    └── gloat tool
        ↓ (dependency)
makeplus/makes (gloat.mk)
    ↓ (build system)
ingydotnet/gist (downstream project)
```

**Key insights**:
- Glojure must be released before gloat (gloat depends on glojure)
- Gloat's `ys/pkg` must be released before gloat itself (published first)
- Makes must be updated (if needed) before downstream releases
- Downstream projects depend on published gloat + ys/pkg versions

## When to Release What

### Glojure Releases

**Release glojure when**:
- You've added gloat-specific patches or bug fixes
- You want to pick up upstream changes
- Gloat needs a new feature or fix from glojure
- You're doing a coordinated ecosystem release

**Skip if**:
- No glojure changes since last release
- Gloat doesn't need updated glojure

**See**: `doc/releasing-glojure.md`

### Gloat Releases

**Release gloat when**:
- You've added new features to gloat
- You've updated the YS standard library
- You've fixed bugs in gloat
- You're doing a coordinated ecosystem release
- Downstream projects need new gloat functionality

**Dependencies**:
- Glojure must be released first (if updated)
- `ys/pkg` is released as part of the gloat release process

**See**: `doc/releasing-gloat.md`

### Downstream Project Releases (Gist, etc.)

**Release downstream projects when**:
- You've updated project code
- You want to pick up new gloat features
- You've updated the gloat version
- You want to publish new binaries

**Dependencies**:
- Gloat must be released first (if updated)
- `ys/pkg` must be on Go proxy
- Makes should be updated (if gloat.mk changed)

**See**: `doc/releasing-gist.md`

## Common Release Scenarios

### Scenario 1: Full Ecosystem Release

**When**: Major feature added that touches all layers.

**Order**:
1. Release glojure (if updated)
2. Update gloat to use new glojure
3. Release gloat (including `ys/pkg`)
4. Update Makes (if `gloat.mk` changed)
5. Release downstream projects with new gloat

**Timeline**: 1-2 hours

**Example**:
```bash
# 1. Release glojure
cd /path/to/glojure
make clean && make all && make test
git tag -a v0.6.6 -m "Release v0.6.6"
git push gloathub gloat && git push gloathub v0.6.6

# 2. Update gloat to use new glojure
cd /path/to/gloat
# Edit common/common.mk: GLOJURE-VERSION := v0.6.6
# Edit ys/pkg/go.mod: require github.com/gloathub/glojure v0.6.6
make update && make ys-pkg && make test
git commit -am "Update to glojure v0.6.6"

# 3. Release gloat
# Edit versions in bin/gloat, Makefile, Changes
git commit -am "Bump version to v0.2.0"
make tag-ys-pkg
git push origin main --tags
# Wait for Go proxy...
git tag -a v0.2.0 -m "Release v0.2.0"
git push origin v0.2.0

# 4. Update Makes (if needed)
cd /tmp && git clone https://github.com/makeplus/makes
cd makes
# Edit gloat.mk if needed
git commit -am "Update for gloat v0.2.0"
git push origin main

# 5. Release gist
cd /path/to/gist
cd .cache/makes && git pull && cd ../..
# Edit Makefile: GLOAT-VERSION := v0.2.0
make gloat-github-release VERSION=0.2.0
```

### Scenario 2: Gloat-Only Release

**When**: Bug fix or feature in gloat, no glojure changes.

**Order**:
1. Release gloat (including `ys/pkg`)
2. Update Makes (if `gloat.mk` changed)
3. Release downstream projects (optional, when ready)

**Timeline**: 30 minutes

**Skip**: Glojure release

### Scenario 3: Downstream-Only Release

**When**: Changes only in a downstream project (e.g., gist).

**Order**:
1. Release the downstream project

**Timeline**: 15 minutes

**Skip**: Glojure and gloat releases

### Scenario 4: Stdlib-Only Update

**When**: Changes only to YS standard library (`ys/src/`).

**Order**:
1. Update and test stdlib in gloat
2. Release gloat (including new `ys/pkg`)
3. Release downstream projects (optional)

**Timeline**: 30 minutes

**Skip**: Glojure release (unless glojure is needed for stdlib changes)

## Version Coordination

### Glojure Versioning

**Scheme**: Follow upstream with suffixes
- `v0.6.5` — Matches upstream exactly
- `v0.6.5-rc1`, `v0.6.5-rc2` — Release candidates
- `v0.6.5.1`, `v0.6.5.2` — Patch releases

**Where it's referenced**:
- Gloat's `common/common.mk`: `GLOJURE-VERSION := v0.6.5-rc4`
- Gloat's `ys/pkg/go.mod`: `require github.com/gloathub/glojure v0.6.5-rc4`

### Gloat Versioning

**Scheme**: Semantic versioning
- `v0.1.0`, `v0.2.0`, `v1.0.0` — Standard semver

**Where it's referenced**:
- `bin/gloat`: `export GLOAT_VERSION=0.1.0`
- `Makefile`: `YS-PKG-VERSION ?= v0.1.0`
- `Changes`: Entry for the version
- Downstream Makefiles: `GLOAT-VERSION := v0.1.0`

### YS Pkg Versioning

**Scheme**: Matches gloat version
- `ys/pkg/v0.1.0` — Sub-module tag

**Where it's referenced**:
- Gloat tag: `ys/pkg/v0.1.0`
- Downstream `go.mod`: `require github.com/gloathub/gloat/ys/pkg v0.1.0`

### Downstream Versioning

**Scheme**: Independent semantic versioning
- `v0.1.5`, `v0.2.0` — Project-specific versions
- `go/v0.1.5` — Go submodule tag

**Where it's referenced**:
- Project Makefile: `VERSION := 0.1.5`
- Source file: `VERSION =: '0.1.5'` (in .ys or .clj)

## Testing Before Release

### Glojure Testing

```bash
cd /path/to/glojure
make clean
make all
make test
```

**What to check**:
- [ ] All platforms build
- [ ] Tests pass (or expected failures match)
- [ ] No new warnings

### Gloat Testing

```bash
cd /path/to/gloat
make update
make ys-pkg
make test
```

**What to check**:
- [ ] Stdlib compiles
- [ ] Generated Go code works
- [ ] All test files compile and run

### Downstream Testing

```bash
cd /path/to/gist
make gloat-go
ls go/
cat go/go.mod
go -C go build ./cmd/gist
./go/gist --version
```

**What to check**:
- [ ] `go/` generates without errors
- [ ] No `replace` directive in `go/go.mod`
- [ ] Binary builds and runs
- [ ] Correct versions in go.mod

## Rollback Strategies

### Glojure Rollback

```bash
cd /path/to/glojure
git tag -d vX.Y.Z
git push gloathub :vX.Y.Z
```

**Impact**: Must also roll back gloat if it depends on the bad version.

### Gloat Rollback

```bash
cd /path/to/gloat
git tag -d vX.Y.Z
git tag -d ys/pkg/vX.Y.Z
git push origin :vX.Y.Z
git push origin :ys/pkg/vX.Y.Z
gh release delete vX.Y.Z
```

**Impact**: Must also roll back downstream projects.

### Downstream Rollback

```bash
cd /path/to/gist
git tag -d vX.Y.Z
git tag -d go/vX.Y.Z
git push origin :vX.Y.Z
git push origin :go/vX.Y.Z
gh release delete vX.Y.Z
```

**Impact**: Only affects the downstream project.

**Important**: Go proxy caches are immutable — rollbacks don't remove published
modules.
You can only publish new versions with fixes.

## Release Checklist Template

Use this checklist for coordinated releases:

### Pre-Release
- [ ] All changes committed and pushed
- [ ] All tests passing
- [ ] Version numbers decided
- [ ] Release notes drafted

### Glojure (if needed)
- [ ] Sync with upstream (if desired)
- [ ] Build: `make clean && make all`
- [ ] Test: `make test`
- [ ] Tag and push
- [ ] Verify on Go proxy

### Gloat
- [ ] Update glojure version (if changed)
- [ ] Rebuild stdlib: `make update && make ys-pkg`
- [ ] Test: `make test`
- [ ] Update version numbers (bin/gloat, Makefile, Changes)
- [ ] Commit version bump
- [ ] Tag ys/pkg: `make tag-ys-pkg`
- [ ] Push: `git push origin main --tags`
- [ ] Verify ys/pkg on Go proxy
- [ ] Tag gloat: `git tag -a vX.Y.Z`
- [ ] Push gloat tag
- [ ] Create GitHub release (optional)

### Makes (if needed)
- [ ] Update gloat.mk
- [ ] Commit and push
- [ ] Verify on GitHub

### Downstream Projects
- [ ] Update Makes cache
- [ ] Update gloat version
- [ ] Test: `make gloat-go`
- [ ] Release: `make gloat-github-release VERSION=X.Y.Z`
- [ ] Verify binaries
- [ ] Test `go install`

### Post-Release
- [ ] Announce release (if appropriate)
- [ ] Update documentation
- [ ] Monitor for issues

## Communication

When releasing, consider:
- **Breaking changes**: Announce ahead of time, document migration path
- **New features**: Update README, create examples
- **Bug fixes**: Document what was fixed
- **Dependencies**: Note required versions (Go, glojure, etc.)

## Frequency

**Recommended cadence**:
- **Glojure**: As needed (when upstream updates or patches required)
- **Gloat**: Monthly or when significant features/fixes accumulate
- **Downstream**: As needed (when project changes or new gloat version needed)

**Coordinate major releases** across all projects quarterly or for significant
features.

## Quick Decision Tree

**Do I need to release glojure?**
- Has glojure code changed? → Yes
- Does gloat need new glojure features? → Yes
- Otherwise → No

**Do I need to release gloat?**
- Has gloat code changed? → Yes
- Has YS stdlib changed? → Yes
- Do downstream projects need new features? → Yes
- Otherwise → No

**Do I need to update Makes?**
- Has gloat.mk changed? → Yes
- Otherwise → No

**Do I need to release downstream project?**
- Has project code changed? → Yes
- Do you want to use new gloat version? → Yes
- Do you need to publish new binaries? → Yes
- Otherwise → No

---

For detailed instructions on each type of release, see:
- **Glojure**: `doc/releasing-glojure.md`
- **Gloat**: `doc/releasing-gloat.md`
- **Downstream Projects**: `doc/releasing-gist.md`

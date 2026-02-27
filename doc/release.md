# Release Guide

## Dependency Flow

```
gloathub/glojure   (fork of glojurelang/glojure)
    ↓
gloathub/gloat     (this repo, includes ys/pkg stdlib)
    ↓
makeplus/makes     (gloat.mk for downstream projects)
    ↓
ingydotnet/gist    (downstream project, etc.)
```

Release order: glojure first (if needed), then gloat, then downstream.

## Releasing Gloat

The normal case — `make release` handles everything automatically:

```bash
make release VERSION=0.1.14
```

If also bumping the Glojure version:

```bash
make release VERSION=0.1.14 GLJ-VERSION=0.6.5-rc7
```

`make release` automatically:

- Updates version in `Makefile`, `bin/gloat`, `Changes`, and docs
- Updates Glojure version in `common/common.mk` and `ys/pkg/go.mod`
  (only when `GLJ-VERSION` is given)
- Runs `make update ys-pkg` and the full test suite
- Commits, tags `ys/pkg/vX.Y.Z`, and pushes
- Waits for Go proxy to index `ys/pkg`
- Tags `vX.Y.Z`, pushes, and creates a GitHub release
- Publishes the website

**Prerequisite**: `~/.github-api-token` present, or `gh` already
authenticated.

## Releasing Glojure (when needed)

Do this **before** a gloat release when the `gloathub/glojure` fork needs
changes.
Work in the glojure repo on the `gloat` branch:

```bash
cd /path/to/glojure
git checkout gloat
make clean && make all && make test
make release VERSION=0.6.5-rc7
```

`make release` builds binaries for `linux_amd64` and `darwin_arm64`,
tags `v0.6.5-rc7`, pushes to the `gloathub` remote, and creates a GitHub
release.

Verify on Go proxy before proceeding:

```bash
GOPROXY=proxy.golang.org go list -m github.com/gloathub/glojure@v0.6.5-rc7
```

Then run the gloat release passing `GLJ-VERSION=0.6.5-rc7`.

## Releasing Downstream Projects (e.g. gist)

For projects using `gloat.mk` from makeplus/makes:

```bash
cd /path/to/gist
make gloat-github-release VERSION=0.1.5
```

This updates versions, regenerates `go/`, commits, tags `vX.Y.Z` and
`go/vX.Y.Z`, pushes, builds cross-platform binaries, and creates a GitHub
release.

**Prerequisites**:

- Gloat's `ys/pkg` must already be published on the Go proxy
- Update the Makes cache if `gloat.mk` changed:
  `cd .cache/makes && git pull && cd ../..`
- Pin the gloat version in the project's `Makefile`:
  `GLOAT-VERSION := v0.1.14`

## Rollback

**Gloat**:

```bash
git tag -d vX.Y.Z && git tag -d ys/pkg/vX.Y.Z
git push origin :vX.Y.Z && git push origin :ys/pkg/vX.Y.Z
gh release delete vX.Y.Z
```

**Glojure**:

```bash
git tag -d vX.Y.Z && git push gloathub :vX.Y.Z
```

**Downstream**:

```bash
git tag -d vX.Y.Z && git tag -d go/vX.Y.Z
git push origin :vX.Y.Z && git push origin :go/vX.Y.Z
gh release delete vX.Y.Z
```

> **Note**: Go proxy caches are immutable — you cannot un-publish a module
> version.
> Publish a new version with fixes instead.

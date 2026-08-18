# Release Guide

## Dependency Flow

```text
glojurelang/glojure
    |
gloathub/ys-v0-go
    |
gloathub/gloat
    |
makeplus/makes and downstream projects
```

Release changed dependencies before Gloat. The `ys-v0-go` runtime is an
independent Go module; Gloat does not tag or publish it.

## Releasing the Runtime

When the portable YAMLScript sources, AOT patches, native backends, or
generated loaders change, work in the `ys-v0-go` repository:

```bash
make generate
make test
make check-generated
```

Commit the source and generated output together, tag the module, publish the
tag, and wait for the Go proxy before updating Gloat:

```bash
GOPROXY=https://proxy.golang.org \
  go list -m github.com/gloathub/ys-v0-go@vX.Y.Z
```

## Releasing Gloat

Pin the published runtime in `common/common.mk`:

```makefile
YS-V0-GO-VERSION := vX.Y.Z
```

Then use the normal release target:

```bash
make release VERSION=0.1.14
```

If Glojure also changes:

```bash
make release VERSION=0.1.14 GLJ-VERSION=0.7.11
```

The release helper verifies that the pinned `ys-v0-go` version resolves from
the public Go proxy, rebuilds generated documentation, runs normal and pruned
test suites, commits the Gloat release files, tags `vX.Y.Z`, pushes, creates
the GitHub release, and publishes the website.

## Releasing Glojure

When Glojure changes, release it before the runtime and Gloat:

```bash
cd /path/to/glojure
make clean && make all && make test
make release VERSION=0.7.11
```

Verify the tag on the Go proxy before using it in `ys-v0-go`:

```bash
GOPROXY=https://proxy.golang.org \
  go list -m github.com/glojurelang/glojure@v0.7.11
```

## Downstream Projects

For projects using `gloat.mk` from Makes:

```bash
make gloat-github-release VERSION=0.1.5
```

Before releasing, update the project to the new Gloat version, regenerate its
Go directory, remove local `replace` directives, and verify that Glojure and
`ys-v0-go` resolve from the Go proxy.

## Rollback

Delete an unpublished or incorrect local tag in the repository that owns it.
If a tag has reached the Go proxy, its contents are immutable: publish a new
version with the correction instead of reusing the version.

# YAMLScript Runtime Patches

This directory contains patches applied to generated Go code after compilation.

## yamlscript-util-seqable.patch

**Purpose**: Fix `seqable?` panic in upstream Glojure

**Problem**: The upstream `clojure/core.glj` defines `seqable?` as
`(clojure.lang.RT/canSeq x)`, which is Java interop that doesn't translate to
Go. The Glojure AOT compiler generates `lang.Apply(nil, ...)` which always
panics at runtime.

**Solution**: Override `clojure.core/seqable?` in the `yamlscript.util`
namespace loader with a working implementation using `lang.CanSeq()`. Since
`yamlscript.util` is loaded before `ys.std` and `ys.dwim`, this ensures all
subsequent uses of `seqable?` work correctly.

**Applied to**: `ys/pkg/yamlscript/util/loader.go`

**Maintenance**: This patch is automatically applied by the Makefile after
generating the loader.go file. If the upstream Glojure adds native support for
`seqable?`, this patch can be removed.

package main

import "C"

import (
	"os"
	"strings"
	"github.com/glojurelang/glojure/pkg/glj"
	"github.com/glojurelang/glojure/pkg/lang"
	_ "GO-MODULE/pkg/PACKAGE-PATH"
YS-IMPORTS
ALL-NS-IMPORTS
)

func init() {
	require := glj.Var("clojure.core", "require")
YS-REQUIRES
ALL-NS-REQUIRES
	require.Invoke(lang.NewSymbol("NAMESPACE"))

	// Set up dynamic variables
	alterVarRoot := glj.Var("clojure.core", "alter-var-root")
	constantly := glj.Var("clojure.core", "constantly")

	// Set *ns* to the user's namespace using thread bindings
	nsObj := lang.FindOrCreateNamespace(lang.NewSymbol("NAMESPACE"))
	nsStarVar := glj.Var("clojure.core", "*ns*")
	pushBindings := glj.Var("clojure.core", "push-thread-bindings")
	bindings := lang.NewMap(nsStarVar, nsObj)
	pushBindings.Invoke(bindings)

	// The YS globals exist only when the library links the YS runtime.
	if lang.FindNamespace(lang.NewSymbol("ys.v0.global")) != nil {
		// ENV: map of all environment variables
		environ := os.Environ()
		envPairs := make([]any, 0, len(environ)*2)
		for _, e := range environ {
			if idx := strings.IndexByte(e, '='); idx >= 0 {
				envPairs = append(envPairs, e[:idx], e[idx+1:])
			}
		}
		envVar := glj.Var("ys.v0.global", "ENV")
		alterVarRoot.Invoke(envVar, constantly.Invoke(lang.NewMap(envPairs...)))

		// CWD: current working directory
		cwd, _ := os.Getwd()
		cwdVar := glj.Var("ys.v0.global", "CWD")
		alterVarRoot.Invoke(cwdVar, constantly.Invoke(cwd))

		// RUN: runtime metadata map (no args for libraries, just pid)
		runMap := lang.NewMap(
			lang.NewKeyword("pid"), int64(os.Getpid()),
		)
		runVar := glj.Var("ys.v0.global", "RUN")
		alterVarRoot.Invoke(runVar, constantly.Invoke(runMap))
	}
	if lang.FindNamespace(lang.NewSymbol("ys.v0")) != nil {
		// NS: the user's namespace object
		nsVar := glj.Var("ys.v0", "NS")
		alterVarRoot.Invoke(nsVar, constantly.Invoke(nsObj))
	}
}

EXPORT-FUNCTIONS

func main() {
	// Required for c-shared build mode
}

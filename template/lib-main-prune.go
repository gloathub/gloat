package main

import "C"

import (
	"os"
	"strings"
	"github.com/gloathub/glojure/pkg/glj"
	"github.com/gloathub/glojure/pkg/lang"
	_ "GO-MODULE/pkg/PACKAGE-PATH"
	_ "GO-MODULE/internal/stdlib/clojure/core"
	_ "github.com/gloathub/glojure/pkg/stdlib/clojure/core/async"
	_ "github.com/gloathub/glojure/pkg/stdlib/clojure/core/protocols"
	_ "github.com/gloathub/glojure/pkg/stdlib/clojure/string"
	_ "github.com/gloathub/glojure/pkg/stdlib/glojure/go/io"
YS-IMPORTS
)

func init() {
	require := glj.Var("clojure.core", "require")
YS-REQUIRES
	require.Invoke(lang.NewSymbol("NAMESPACE"))

	// Set up dynamic variables
	alterVarRoot := glj.Var("clojure.core", "alter-var-root")
	constantly := glj.Var("clojure.core", "constantly")

	// ENV: map of all environment variables
	environ := os.Environ()
	envPairs := make([]any, 0, len(environ)*2)
	for _, e := range environ {
		if idx := strings.IndexByte(e, '='); idx >= 0 {
			envPairs = append(envPairs, e[:idx], e[idx+1:])
		}
	}
	envVar := glj.Var("NAMESPACE", "ENV")
	alterVarRoot.Invoke(envVar, constantly.Invoke(lang.NewMap(envPairs...)))

	// NS: the user's namespace object
	nsVar := glj.Var("NAMESPACE", "NS")
	nsObj := lang.FindOrCreateNamespace(lang.NewSymbol("NAMESPACE"))
	alterVarRoot.Invoke(nsVar, constantly.Invoke(nsObj))

	// Set *ns* to the user's namespace using thread bindings
	nsStarVar := glj.Var("clojure.core", "*ns*")
	pushBindings := glj.Var("clojure.core", "push-thread-bindings")
	bindings := lang.NewMap(nsStarVar, nsObj)
	pushBindings.Invoke(bindings)

	// CWD: current working directory
	cwd, _ := os.Getwd()
	cwdVar := glj.Var("NAMESPACE", "CWD")
	alterVarRoot.Invoke(cwdVar, constantly.Invoke(cwd))

	// RUN: runtime metadata map (no args for libraries, just pid)
	runMap := lang.NewMap(
		lang.NewKeyword("pid"), int64(os.Getpid()),
	)
	runVar := glj.Var("NAMESPACE", "RUN")
	alterVarRoot.Invoke(runVar, constantly.Invoke(runMap))
}

EXPORT-FUNCTIONS

func main() {
	// Required for c-shared build mode
}

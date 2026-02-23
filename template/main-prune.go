package main

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

func main() {
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
	envVar := glj.Var("ys.v0", "ENV")
	alterVarRoot.Invoke(envVar, constantly.Invoke(lang.NewMap(envPairs...)))

	// NS: the user's namespace object
	nsVar := glj.Var("ys.v0", "NS")
	nsObj := lang.FindOrCreateNamespace(lang.NewSymbol("NAMESPACE"))
	alterVarRoot.Invoke(nsVar, constantly.Invoke(nsObj))

	// Set *ns* to the user's namespace using thread bindings
	nsStarVar := glj.Var("clojure.core", "*ns*")
	pushBindings := glj.Var("clojure.core", "push-thread-bindings")
	bindings := lang.NewMap(nsStarVar, nsObj)
	pushBindings.Invoke(bindings)

	// CWD: current working directory
	cwd, _ := os.Getwd()
	cwdVar := glj.Var("ys.v0", "CWD")
	alterVarRoot.Invoke(cwdVar, constantly.Invoke(cwd))

	// RUN: runtime metadata map (includes args and pid)
	args := os.Args[1:]
	anyArgs := make([]any, len(args))
	for i, arg := range args {
		anyArgs[i] = arg
	}
	argsVec := lang.NewVector(anyArgs...)
	runMap := lang.NewMap(
		lang.NewKeyword("args"), argsVec,
		lang.NewKeyword("pid"), int64(os.Getpid()),
	)
	runVar := glj.Var("ys.v0", "RUN")
	alterVarRoot.Invoke(runVar, constantly.Invoke(runMap))

	// ARGV and ARGS are set in -main function itself
	// Call -main with args
	myMain := glj.Var("NAMESPACE", "-main")
	myMain.Invoke(anyArgs...)
}

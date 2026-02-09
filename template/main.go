package main

import (
	"os"
	"strings"
	"github.com/glojurelang/glojure/pkg/glj"
	"github.com/glojurelang/glojure/pkg/lang"
	_ "build.yamlscript.org/MODULE-NAME/pkg/PACKAGE-PATH"
	_ "build.yamlscript.org/MODULE-NAME/pkg/yamlscript/util"
	_ "build.yamlscript.org/MODULE-NAME/pkg/ys/fs"
	_ "build.yamlscript.org/MODULE-NAME/pkg/ys/ipc"
	_ "build.yamlscript.org/MODULE-NAME/pkg/ys/std"
	_ "build.yamlscript.org/MODULE-NAME/pkg/ys/dwim"
	_ "build.yamlscript.org/MODULE-NAME/pkg/ys/v0"
)

func main() {
	require := glj.Var("clojure.core", "require")
	require.Invoke(lang.NewSymbol("yamlscript.util"))
	require.Invoke(lang.NewSymbol("ys.fs"))
	require.Invoke(lang.NewSymbol("ys.ipc"))
	require.Invoke(lang.NewSymbol("ys.std"))
	require.Invoke(lang.NewSymbol("ys.dwim"))
	require.Invoke(lang.NewSymbol("ys.v0"))
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
	runVar := glj.Var("NAMESPACE", "RUN")
	alterVarRoot.Invoke(runVar, constantly.Invoke(runMap))

	// ARGV and ARGS are set in -main function itself
	// Call -main with args
	myMain := glj.Var("NAMESPACE", "-main")
	myMain.Invoke(anyArgs...)
}

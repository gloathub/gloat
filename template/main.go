package main

import (
	"os"
	"github.com/glojurelang/glojure/pkg/glj"
	"github.com/glojurelang/glojure/pkg/lang"
	_ "build.yamlscript.org/MODULE-NAME/pkg/PACKAGE-PATH"
	_ "build.yamlscript.org/MODULE-NAME/pkg/ys/std"
	_ "build.yamlscript.org/MODULE-NAME/pkg/ys/dwim"
	_ "build.yamlscript.org/MODULE-NAME/pkg/ys/v0"
)

func main() {
	require := glj.Var("clojure.core", "require")
	require.Invoke(lang.NewSymbol("ys.std"))
	require.Invoke(lang.NewSymbol("ys.dwim"))
	require.Invoke(lang.NewSymbol("ys.v0"))
	require.Invoke(lang.NewSymbol("NAMESPACE"))
	myMain := glj.Var("NAMESPACE", "-main")
	args := os.Args[1:]
	anyArgs := make([]any, len(args))
	for i, arg := range args {
		anyArgs[i] = arg
	}
	myMain.Invoke(anyArgs...)
}

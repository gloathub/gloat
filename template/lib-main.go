package main

import "C"

import (
	"github.com/glojurelang/glojure/pkg/glj"
	"github.com/glojurelang/glojure/pkg/lang"
	_ "build.yamlscript.org/MODULE-NAME/PACKAGE-PATH"
	_ "build.yamlscript.org/MODULE-NAME/ys/std"
	_ "build.yamlscript.org/MODULE-NAME/ys/dwim"
	_ "build.yamlscript.org/MODULE-NAME/ys/v0"
)

func init() {
	require := glj.Var("clojure.core", "require")
	require.Invoke(lang.NewSymbol("ys.std"))
	require.Invoke(lang.NewSymbol("ys.dwim"))
	require.Invoke(lang.NewSymbol("ys.v0"))
	require.Invoke(lang.NewSymbol("NAMESPACE"))
}

// Export functions will be added here by code inspection
// For now, just export the -main function as an example

//export Main
func Main(argc C.int, argv **C.char) {
	// TODO: Parse C args and call -main
	myMain := glj.Var("NAMESPACE", "-main")
	myMain.Invoke()
}

func main() {
	// Required for c-shared build mode
}

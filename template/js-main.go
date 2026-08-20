package main

import (
	"fmt"
	"os"
	"strings"
	"syscall/js"

	"github.com/glojurelang/glojure/pkg/glj"
	"github.com/glojurelang/glojure/pkg/lang"
	ysv0 "github.com/gloathub/ys-v0-glj/runtime"
	_ "GO-MODULE/pkg/PACKAGE-PATH"
ALL-NS-IMPORTS
)

const programNamespace = "NAMESPACE"

var exportFunctions []js.Func

func convertArgument(value js.Value, valueType string) any {
	switch valueType {
	case "str":
		return value.String()
	case "int":
		return int64(value.Int())
	case "float":
		return value.Float()
	case "bool":
		return value.Bool()
	case "null":
		return nil
	default:
		panic(fmt.Sprintf("unsupported EXPORT argument type %q", valueType))
	}
}

func convertResult(value any, valueType string) any {
	switch valueType {
	case "str":
		result, ok := value.(string)
		if !ok {
			panic(fmt.Sprintf("expected string result, got %T", value))
		}
		return result
	case "int":
		result, ok := value.(int64)
		if !ok {
			panic(fmt.Sprintf("expected integer result, got %T", value))
		}
		return result
	case "float":
		result, ok := value.(float64)
		if !ok {
			panic(fmt.Sprintf("expected float result, got %T", value))
		}
		return result
	case "bool":
		result, ok := value.(bool)
		if !ok {
			panic(fmt.Sprintf("expected boolean result, got %T", value))
		}
		return result
	case "null":
		return nil
	default:
		panic(fmt.Sprintf("unsupported EXPORT result type %q", valueType))
	}
}

func exportError(value any) string {
	if err, ok := value.(error); ok {
		return err.Error()
	}
	return fmt.Sprint(value)
}

func registerExport(
	exports map[string]any,
	name string,
	argumentTypes []string,
	resultType string,
) {
	function := js.FuncOf(func(_ js.Value, arguments []js.Value) (result any) {
		defer func() {
			if recovered := recover(); recovered != nil {
				result = map[string]any{
					"ok":    false,
					"error": exportError(recovered),
				}
			}
		}()
		if len(arguments) != len(argumentTypes) {
			panic(fmt.Sprintf(
				"%s expects %d arguments, got %d",
				name,
				len(argumentTypes),
				len(arguments),
			))
		}
		values := make([]any, len(arguments))
		for index, argument := range arguments {
			values[index] = convertArgument(argument, argumentTypes[index])
		}
		value := glj.Var(programNamespace, name).Invoke(values...)
		return map[string]any{
			"ok":    true,
			"value": convertResult(value, resultType),
		}
	})
	exportFunctions = append(exportFunctions, function)
	exports[name] = function
}

func initializeRuntime() {
	ysv0.Load()
	require := glj.Var("clojure.core", "require")
ALL-NS-REQUIRES
	require.Invoke(lang.NewSymbol(programNamespace))

	alterVarRoot := glj.Var("clojure.core", "alter-var-root")
	constantly := glj.Var("clojure.core", "constantly")

	environ := os.Environ()
	envPairs := make([]any, 0, len(environ)*2)
	for _, entry := range environ {
		if index := strings.IndexByte(entry, '='); index >= 0 {
			envPairs = append(envPairs, entry[:index], entry[index+1:])
		}
	}
	alterVarRoot.Invoke(
		glj.Var("ys.v0.global", "ENV"),
		constantly.Invoke(lang.NewMap(envPairs...)),
	)

	namespace := lang.FindOrCreateNamespace(lang.NewSymbol(programNamespace))
	alterVarRoot.Invoke(glj.Var("ys.v0", "NS"), constantly.Invoke(namespace))
	glj.Var("clojure.core", "push-thread-bindings").Invoke(
		lang.NewMap(glj.Var("clojure.core", "*ns*"), namespace),
	)

	cwd, _ := os.Getwd()
	alterVarRoot.Invoke(glj.Var("ys.v0.global", "CWD"), constantly.Invoke(cwd))
	alterVarRoot.Invoke(
		glj.Var("ys.v0.global", "RUN"),
		constantly.Invoke(lang.NewMap(
			lang.NewKeyword("args"), lang.NewVector(),
			lang.NewKeyword("pid"), int64(os.Getpid()),
		)),
	)

	// Load dependencies requested by portable use forms.
PORTABLE-USE-LOADS
}

func main() {
	initializeRuntime()
	exportObject := map[string]any{}
EXPORT-FUNCTIONS
	js.Global().Set("gloat", map[string]any{
		"namespace": programNamespace,
		"exports":   exportObject,
	})
	dispatch := js.Global().Get("dispatchEvent")
	if dispatch.Type() == js.TypeFunction {
		if event := js.Global().Get("Event"); event.Type() == js.TypeFunction {
			dispatch.Invoke(event.New("gloat-ready"))
		}
	}
	select {}
}

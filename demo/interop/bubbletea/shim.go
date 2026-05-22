// Bubbletea interop shim for the picker demo.
//
// All application logic lives in picker.clj; this shim only implements
// tea.Model and delegates each callback to a Clojure var via glj.Var.
// To build the demo: run gloat on picker.clj with
// --module=gloat-demo/picker, copy this file over the generated
// build/main.go, and `go build` from the build directory.
// The accompanying Makefile is a convenience wrapper around those
// steps; the shim itself is plain Go.
package main

import (
	"fmt"
	"os"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/gloathub/glojure/pkg/glj"
	"github.com/gloathub/glojure/pkg/lang"
	// Blank-import the gloat-compiled picker.core package so its init()
	// runs and registers the namespace loader. The path matches gloat's
	// --module value plus the namespace path under pkg/.
	_ "gloat-demo/picker/pkg/picker/core"
	_ "github.com/gloathub/gloat/ys/pkg/all"
)

var (
	initModelFn    lang.IFn
	updateModelFn  lang.IFn
	viewModelFn    lang.IFn
	formatResultFn lang.IFn
)

type model struct {
	state interface{}
}

func (m model) Init() tea.Cmd {
	return nil
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		result := updateModelFn.Invoke(m.state, msg.String())
		if result == nil {
			return m, tea.Quit
		}
		next := model{state: result}
		if done := formatResultFn.Invoke(result); done != nil {
			return next, tea.Quit
		}
		return next, nil
	}
	return m, nil
}

func (m model) View() string {
	result := viewModelFn.Invoke(m.state)
	if s, ok := result.(string); ok {
		return s
	}
	return fmt.Sprintf("%v", result)
}

func main() {
	require := glj.Var("clojure.core", "require")
	require.Invoke(lang.NewSymbol("ys.std"))
	require.Invoke(lang.NewSymbol("ys.dwim"))
	require.Invoke(lang.NewSymbol("ys.v0"))
	require.Invoke(lang.NewSymbol("picker.core"))

	initModelFn = glj.Var("picker.core", "init-model")
	updateModelFn = glj.Var("picker.core", "update-model")
	viewModelFn = glj.Var("picker.core", "view-model")
	formatResultFn = glj.Var("picker.core", "format-result")

	initialState := initModelFn.Invoke()
	p := tea.NewProgram(model{state: initialState})
	finalModel, err := p.Run()
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	if m, ok := finalModel.(model); ok {
		if result := formatResultFn.Invoke(m.state); result != nil {
			if s, ok := result.(string); ok {
				fmt.Println(s)
			}
		}
	}
}

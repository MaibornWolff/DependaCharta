package main

import (
	"encoding/json"
	"fmt"
	"go/token"
	"log"
	"os"
	"os/exec"
	"strings"
)

type GoDictionaryData struct {
	Keywords   []string            `json:"keywords"`
	Builtins   []string            `json:"builtins"`
	StdLibs    []string            `json:"stdlibs"`
	Dictionary map[string][]string `json:"dictionary"`
}

func main() {
	data := GoDictionaryData{
		Dictionary: make(map[string][]string),
	}

	var keywords []string
	for i := token.BREAK; i <= token.VAR; i++ {
		if token.IsKeyword(i.String()) {
			keywords = append(keywords, i.String())
		}
	}
	data.Keywords = keywords

	builtins := []string{
		// Types
		"bool", "byte", "complex64", "complex128", "error", "float32", "float64",
		"int", "int8", "int16", "int32", "int64", "rune",
		"string", "uint", "uint8", "uint16", "uint32", "uint64", "uintptr",
		"any", "comparable",
		// Constants
		"true", "false", "iota", "nil",
		"append", "cap", "close", "complex", "copy", "delete", "imag", "len",
		"make", "new", "panic", "print", "println", "real", "recover",
		"clear", "max", "min",
	}
	data.Builtins = builtins

	cmd := exec.Command("go", "list", "std")
	out, err := cmd.CombinedOutput()
	if err != nil {
		log.Fatalf("Failed to get standard library packages: %v", err)
	}

	stdPkgs := strings.Fields(string(out))
	var filteredStdPkgs []string
	for _, pkg := range stdPkgs {
		if !strings.Contains(pkg, "internal") && !strings.Contains(pkg, "vendor") {
			filteredStdPkgs = append(filteredStdPkgs, pkg)
		}
	}
	data.StdLibs = filteredStdPkgs

	for _, keyword := range keywords {
		data.Dictionary[keyword] = []string{keyword}
	}

	for _, builtin := range builtins {
		data.Dictionary[builtin] = []string{builtin}
	}

	for _, pkg := range filteredStdPkgs {
		internalPath := strings.ReplaceAll(pkg, "/", ".")

		data.Dictionary[pkg] = strings.Split(internalPath, ".")

		lastComponent := pkg
		if idx := strings.LastIndex(pkg, "/"); idx != -1 {
			lastComponent = pkg[idx+1:]
		}

		if _, exists := data.Dictionary[lastComponent]; !exists {
			data.Dictionary[lastComponent] = strings.Split(internalPath, ".")
		}
	}

	encoder := json.NewEncoder(os.Stdout)
	encoder.SetIndent("", "  ")
	if err := encoder.Encode(data); err != nil {
		log.Fatalf("Failed to encode JSON: %v", err)
	}

	fmt.Fprintf(os.Stderr, "Generated Go dictionary with:\n")
	fmt.Fprintf(os.Stderr, "- %d keywords\n", len(keywords))
	fmt.Fprintf(os.Stderr, "- %d builtin identifiers\n", len(builtins))
	fmt.Fprintf(os.Stderr, "- %d standard library packages\n", len(filteredStdPkgs))
	fmt.Fprintf(os.Stderr, "- %d total dictionary entries\n", len(data.Dictionary))
}

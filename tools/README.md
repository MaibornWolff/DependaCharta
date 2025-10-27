# Tools Directory

## Go Dictionary Generator

The `gen_go_dictionary.go` script generates comprehensive Go dictionary data for the dependency analyzer.

### Usage

Ensure you have go installed on your system.

To regenerate the dictionary data (for Go version updates):

```bash
cd /tmp
go run /path/to/codegraph/tools/gen_go_dictionary.go > /path/to/codegraph/tools/go_dictionary.json
```

### How to get from the json to the kotlin code

Use an AI Agent / LLM of you choice and generate the complete file.

### Generated Data

The script generates:
- **25 keywords** (break, case, chan, const, etc.)
- **44 builtin identifiers** (types like `int`, `string`; constants like `nil`, `true`; functions like `append`, `make`)
- **172+ standard library packages** (automatically discovered via `go list std`)

Total: **369+ dictionary entries** including both full package paths (`net/http`) and short names (`http`).

### Integration

The generated JSON data should be manually integrated into `GoDictionary.kt`. This ensures the dictionary stays current with Go releases while avoiding runtime dependencies on Go toolchain.

### Why this approach?

- **Self-maintaining**: Automatically discovers current Go version's stdlib
- **Complete**: Covers all categories (keywords, builtins, stdlib) 
- **Accurate**: Generated from official Go toolchain
- **Portable**: No runtime Go dependency in analyzer 
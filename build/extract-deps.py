#!/usr/bin/env python3

import re
import sys
import yaml
from collections import defaultdict

LOADER_FILE = "/home/ingy/src/gloat/.cache/.local/cache/glojure-v0.6.4/pkg/stdlib/clojure/core/loader.go"
OUTPUT_FILE = "clojure-core.yaml"

# Special forms to track
SPECIAL_FORMS = {
    "if", "do", "let", "fn", "def", "quote", "loop", "recur",
    "throw", "try", "catch", "finally", "var", "set!", "new", ".", "letfn", "case*"
}

def decode_name(encoded):
    """Decode Go-encoded Clojure names"""
    replacements = {
        '_DASH_': '-',
        '_QMARK_': '?',
        '_BANG_': '!',
        '_STAR_': '*',
        '_PLUS_': '+',
        '_GT_': '>',
        '_LT_': '<',
        '_EQ_': '=',
        '_SLASH_': '/',
        '_DOT_': '.',
        '_TICK_': "'",
        '_AMP_': '&',
        '_PCT_': '%',
        '_COLON_': ':',
    }
    result = encoded
    for pattern, replacement in replacements.items():
        result = result.replace(pattern, replacement)
    return result

def parse_file(file_path):
    """Parse the loader.go file into blocks"""
    with open(file_path, 'r') as f:
        lines = f.readlines()

    blocks = {}
    current_name = None
    block_start = None
    brace_count = 0
    i = 0

    while i < len(lines):
        line = lines[i]

        # Check for definition comment
        match = re.match(r'^\t// (\S+)$', line)
        if match:
            current_name = match.group(1)
            i += 1
            continue

        # Check for opening brace
        if current_name and brace_count == 0 and re.match(r'^\t\{', line):
            block_start = i
            brace_count = 1
            i += 1
            continue

        # Track braces inside block
        if current_name and brace_count > 0:
            opens = line.count('{')
            closes = line.count('}')
            brace_count += opens - closes

            if brace_count == 0:
                # Block complete
                block_lines = lines[block_start:i+1]
                block = ''.join(block_lines)
                blocks[current_name] = block
                current_name = None
                block_start = None

        i += 1

    return blocks

def is_macro(block):
    """Check if a definition is a macro"""
    return 'kw_macro, true' in block

def extract_fn_deps(block, macros):
    """Extract function dependencies from checkDerefVar calls"""
    pattern = r'checkDerefVar\(var_clojure_DOT_core_(\w+)\)'
    matches = re.findall(pattern, block)

    deps = set()
    for match in matches:
        decoded = decode_name(match)
        if decoded in macros:
            deps.add(f"{decoded}/M")
        else:
            deps.add(decoded)

    return deps

def extract_special_form_deps(block):
    """Extract special form dependencies from sym_ usage in lang.Apply"""
    pattern = r'lang\.Apply\([^,]+,\s*\[\]any\{sym_(\w+)'
    matches = re.findall(pattern, block)

    deps = set()
    for match in matches:
        decoded = decode_name(match)
        if decoded in SPECIAL_FORMS:
            deps.add(f"{decoded}/S")

    return deps

def build_dep_graph(blocks):
    """Build the complete dependency graph"""
    # First pass: identify macros
    macros = {name for name, block in blocks.items() if is_macro(block)}

    # Second pass: extract dependencies
    deps = {}
    for name, block in blocks.items():
        fn_deps = extract_fn_deps(block, macros)
        sf_deps = extract_special_form_deps(block)
        all_deps = fn_deps | sf_deps

        if all_deps:
            full_name = f"{name}/M" if name in macros else name
            deps[full_name] = sorted(all_deps)

    return deps

def format_yaml(deps):
    """Format dependency graph as YAML using proper YAML library"""
    # Convert to ordered dict for nicer output
    sorted_deps = {k: deps[k] for k in sorted(deps.keys())}
    return yaml.dump(sorted_deps, default_flow_style=False, allow_unicode=True, sort_keys=False)

def main():
    print("Parsing loader.go...")
    blocks = parse_file(LOADER_FILE)
    print(f"Found {len(blocks)} definitions")

    print("Building dependency graph...")
    deps = build_dep_graph(blocks)
    print(f"Found {len(deps)} definitions with dependencies")

    print(f"Writing {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, 'w') as f:
        f.write(format_yaml(deps))

    print("Done!")

if __name__ == '__main__':
    main()

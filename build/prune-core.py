#!/usr/bin/env python3

"""
prune-core.py - Generate a pruned clojure/core/loader.go

Scans Go source files for clojure.core function references,
computes the transitive dependency closure using the dependency graph,
and generates a pruned loader.go containing only needed functions.
"""

import argparse
import os
import re
import sys
import yaml


# Go-encoded name → Clojure name mapping
GO_DECODE = {
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


def decode_go_name(encoded):
    """Decode a Go-encoded Clojure name."""
    result = encoded
    for pattern, replacement in GO_DECODE.items():
        result = result.replace(pattern, replacement)
    return result


def scan_go_files(paths, loader_path):
    """Scan Go files for var_clojure_DOT_core_XXX references.

    Returns set of decoded Clojure function names.
    """
    # Normalize loader path for comparison
    loader_norm = os.path.normpath(loader_path)

    pattern = re.compile(r'var_clojure_DOT_core_(\w+)')
    refs = set()

    for path in paths:
        if os.path.isfile(path):
            files = [path]
        elif os.path.isdir(path):
            files = []
            for root, _, filenames in os.walk(path):
                for fn in filenames:
                    if fn.endswith('.go'):
                        files.append(os.path.join(root, fn))
        else:
            continue

        for filepath in files:
            # Skip the loader.go file itself
            if os.path.normpath(filepath) == loader_norm:
                continue
            try:
                with open(filepath, 'r') as f:
                    content = f.read()
                for match in pattern.finditer(content):
                    name = decode_go_name(match.group(1))
                    refs.add(name)
            except (IOError, UnicodeDecodeError):
                continue

    return refs


def load_dep_graph(graph_path):
    """Load the dependency graph from YAML file.

    Returns dict mapping function name → set of dependency names.
    """
    with open(graph_path, 'r') as f:
        raw = yaml.safe_load(f)

    graph = {}
    for key, deps in (raw or {}).items():
        graph[key] = set(deps) if deps else set()

    return graph


def compute_closure(root_set, graph):
    """Compute transitive closure of dependencies.

    Skips /S entries (special forms).
    Strips /M suffixes (macros are regular blocks in loader.go).
    """
    keep = set()
    worklist = list(root_set)

    while worklist:
        name = worklist.pop()

        # Skip special forms
        if name.endswith('/S'):
            continue

        # Strip /M suffix for the keep set
        base_name = name[:-2] if name.endswith('/M') else name

        if base_name in keep:
            continue
        keep.add(base_name)

        # Look up deps for both base and /M variant
        for lookup in [base_name, f"{base_name}/M"]:
            if lookup in graph:
                for dep in graph[lookup]:
                    dep_base = dep[:-2] if dep.endswith('/M') else dep
                    if dep_base not in keep and not dep.endswith('/S'):
                        worklist.append(dep)

    return keep


def parse_loader(loader_path):
    """Parse loader.go into structured sections.

    Returns dict with:
      - header_lines: lines before func LoadNS()
      - loadns_open: the 'func LoadNS() {' line
      - sym_lines: list of (line_text, var_name) for sym_ declarations
      - kw_lines: list of (line_text, var_name) for kw_ declarations
      - var_lines: list of (line_text, var_name, comment_line) for var_ declarations
      - preamble_lines: lines between var declarations and first function block
      - blocks: list of (name, block_text) for function blocks
      - closing: closing brace line
    """
    with open(loader_path, 'r') as f:
        lines = f.readlines()

    result = {
        'header_lines': [],
        'loadns_open': '',
        'sym_lines': [],
        'kw_lines': [],
        'var_lines': [],
        'preamble_lines': [],
        'blocks': [],
        'closing': '}\n',
    }

    # Phase 1: Find header (everything before func LoadNS)
    i = 0
    while i < len(lines):
        if lines[i].startswith('func LoadNS()'):
            result['loadns_open'] = lines[i]
            i += 1
            break
        result['header_lines'].append(lines[i])
        i += 1

    # Phase 2: Parse sym_, kw_, var_ declarations
    while i < len(lines):
        line = lines[i]

        # sym_ declaration
        sym_match = re.match(r'\t(sym_\w+)\s*:=\s*lang\.NewSymbol\(', line)
        if sym_match:
            result['sym_lines'].append((line, sym_match.group(1)))
            i += 1
            continue

        # kw_ declaration
        kw_match = re.match(r'\t(kw_\w+)\s*:=\s*lang\.NewKeyword\(', line)
        if kw_match:
            result['kw_lines'].append((line, kw_match.group(1)))
            i += 1
            continue

        # var_ declaration with preceding // var comment
        var_comment = re.match(r'\t// var ', line)
        if var_comment:
            comment_line = line
            i += 1
            if i < len(lines):
                var_match = re.match(r'\t(var_\w+)\s*:=\s*lang\.InternVarName\(', lines[i])
                if var_match:
                    result['var_lines'].append((lines[i], var_match.group(1), comment_line))
                    i += 1
                    continue
                else:
                    # Comment without matching var declaration - put in preamble
                    result['preamble_lines'].append(comment_line)
                    continue
            else:
                result['preamble_lines'].append(comment_line)
                break

        # Not a declaration - we've reached the preamble
        break

    # Phase 3: Parse preamble (between declarations and function blocks)
    # Also parse closed variable blocks and function blocks
    while i < len(lines):
        line = lines[i]

        # Check if this is a function block start (// name followed by {)
        block_match = re.match(r'^\t// (\S+)$', line)
        if block_match and i + 1 < len(lines) and re.match(r'^\t\{', lines[i + 1]):
            # This is either a function block or we need to check if it's
            # a block comment for var declarations we already passed
            break

        result['preamble_lines'].append(line)
        i += 1

    # Phase 4: Parse function blocks
    while i < len(lines):
        line = lines[i]

        # Check for closing brace of LoadNS
        if line == '}\n' or line == '}':
            result['closing'] = line
            break

        # Check for function block marker
        block_match = re.match(r'^\t// (\S+)$', line)
        if block_match and i + 1 < len(lines) and re.match(r'^\t\{', lines[i + 1]):
            name = block_match.group(1)
            block_lines = [line]  # include the // comment
            i += 1

            # Track brace depth
            brace_depth = 0
            while i < len(lines):
                block_lines.append(lines[i])
                brace_depth += lines[i].count('{') - lines[i].count('}')
                if brace_depth == 0:
                    i += 1
                    break
                i += 1

            result['blocks'].append((name, ''.join(block_lines)))
            continue

        # Non-block line in function section - append to last block or preamble
        # This handles the closed variable blocks in the preamble
        result['preamble_lines'].append(line)
        i += 1

    return result


def find_used_identifiers(code_text):
    """Find all sym_, kw_, var_, closed identifiers used in code."""
    syms = set(re.findall(r'\b(sym_\w+)\b', code_text))
    kws = set(re.findall(r'\b(kw_\w+)\b', code_text))
    vars_ = set(re.findall(r'\b(var_\w+)\b', code_text))
    closed = set(re.findall(r'\b(closed\d+)\b', code_text))
    return syms, kws, vars_, closed


def prune_closed_vars(preamble_text, used_closed):
    """Remove unused closed variable blocks from preamble.

    Closed var blocks look like:
        var closedN any
        {
            closedN = ...
        }
    """
    lines = preamble_text.split('\n')
    result = []
    i = 0
    while i < len(lines):
        # Check for closed var declaration
        closed_match = re.match(r'^\tvar (closed\d+) any', lines[i])
        if closed_match:
            var_name = closed_match.group(1)
            if var_name not in used_closed:
                # Skip this declaration and its block
                i += 1  # skip 'var closedN any'
                if i < len(lines) and lines[i].strip() == '{':
                    depth = 1
                    i += 1
                    while i < len(lines) and depth > 0:
                        depth += lines[i].count('{') - lines[i].count('}')
                        i += 1
                continue
        result.append(lines[i])
        i += 1
    return '\n'.join(result)


def find_used_imports(full_code):
    """Determine which imports are actually used in the code.

    Returns set of import alias names that appear in the code body.
    """
    # Parse import block from header
    import_pattern = re.compile(r'^\t(\w+)\s+"[^"]+"\s*$', re.MULTILINE)
    # Check each alias for usage in the code
    used = set()
    for m in import_pattern.finditer(full_code):
        alias = m.group(1)
        # Check if alias is used outside the import block
        # Use word boundary to match alias usage like: alias.Something
        usage = re.compile(r'\b' + re.escape(alias) + r'\b\.')
        if usage.search(full_code[m.end():]):
            used.add(alias)
    return used


def prune_imports(header_lines, used_imports):
    """Remove unused imports from the header.

    Always keep 'lang' and 'runtime' as they're essential.
    """
    always_keep = {'lang', 'runtime', 'fmt'}
    result = []
    in_import = False
    import_depth = 0

    for line in header_lines:
        if 'import (' in line:
            in_import = True
            import_depth = 1
            result.append(line)
            continue

        if in_import:
            import_depth += line.count('(') - line.count(')')
            if import_depth <= 0:
                in_import = False
                result.append(line)
                continue

            # Check if this import line should be kept
            import_match = re.match(r'\t(\w+)\s+"', line)
            if import_match:
                alias = import_match.group(1)
                if alias in always_keep or alias in used_imports:
                    result.append(line)
                # else: skip unused import
            else:
                result.append(line)
            continue

        result.append(line)

    return result


def generate_pruned_loader(parsed, keep_set):
    """Generate pruned loader.go content."""

    # Step 1: Filter function blocks
    kept_blocks = []
    for name, block_text in parsed['blocks']:
        if name in keep_set:
            kept_blocks.append((name, block_text))

    # Step 2: Prune closed variables from preamble first, since they
    # self-reference and can keep syms/vars alive that nothing else needs
    all_kept_code = ''.join(text for _, text in kept_blocks)
    preamble_text = ''.join(parsed['preamble_lines'])

    # Find which closed vars are actually used by kept function blocks
    _, _, _, used_closed = find_used_identifiers(all_kept_code)
    pruned_preamble = prune_closed_vars(preamble_text, used_closed)

    # Step 3: Find used var_ identifiers from blocks + pruned preamble
    code_for_var_scan = pruned_preamble + all_kept_code
    _, _, used_vars, _ = find_used_identifiers(code_for_var_scan)

    # Step 4: Filter var declarations based on usage
    kept_var_lines = []
    for line_text, var_name, comment_line in parsed['var_lines']:
        if var_name in used_vars:
            kept_var_lines.append((line_text, var_name, comment_line))

    # Step 5: Scan kept blocks + pruned preamble + kept var declarations
    # for used sym_ and kw_ identifiers
    kept_var_text = ''.join(lt for lt, _, _ in kept_var_lines)
    kept_var_text += ''.join(cl for _, _, cl in kept_var_lines)
    all_scannable = code_for_var_scan + kept_var_text
    used_syms, used_kws, _, _ = find_used_identifiers(all_scannable)

    # Step 6: Filter sym declarations
    kept_syms = []
    for line_text, var_name in parsed['sym_lines']:
        if var_name in used_syms:
            kept_syms.append(line_text)

    # Step 7: Filter kw declarations
    kept_kws = []
    for line_text, var_name in parsed['kw_lines']:
        if var_name in used_kws:
            kept_kws.append(line_text)

    # Step 8: Format kept var declarations
    kept_vars = []
    for line_text, var_name, comment_line in kept_var_lines:
        kept_vars.append(comment_line)
        kept_vars.append(line_text)

    # Step 9: Assemble the output
    blocks_text = ''.join(text for _, text in kept_blocks)

    # Step 10: Determine used imports from the full output
    header_text = ''.join(parsed['header_lines'])
    body_code = (
        ''.join(kept_syms)
        + ''.join(kept_kws)
        + ''.join(kept_vars)
        + pruned_preamble
        + blocks_text
    )
    full_text = header_text + parsed['loadns_open'] + body_code + parsed['closing']
    used_imports = find_used_imports(full_text)

    # Step 11: Prune imports from header
    pruned_header = prune_imports(parsed['header_lines'], used_imports)

    # Step 12: Final assembly
    output = (
        ''.join(pruned_header)
        + parsed['loadns_open']
        + ''.join(kept_syms)
        + ''.join(kept_kws)
        + ''.join(kept_vars)
        + pruned_preamble
        + blocks_text
        + parsed['closing']
    )

    return output, len(kept_blocks)


def main():
    parser = argparse.ArgumentParser(
        description='Generate pruned clojure/core/loader.go')
    parser.add_argument('--loader', required=True,
                        help='Path to original clojure/core/loader.go')
    parser.add_argument('--graph', required=True,
                        help='Path to clojure-core.yaml dependency graph')
    parser.add_argument('--scan', action='append', default=[],
                        help='Paths to scan for clojure.core refs (repeatable)')
    parser.add_argument('--keep', action='append', default=[],
                        help='Extra function names to always keep (repeatable)')
    parser.add_argument('--keep-macros', action='store_true',
                        help='Keep all macros (needed for runtime .glj eval)')
    parser.add_argument('--output', required=True,
                        help='Where to write pruned loader.go')
    parser.add_argument('--verbose', action='store_true',
                        help='Print detailed information')

    args = parser.parse_args()

    # Step 1: Scan for references
    if args.verbose:
        print("Scanning for clojure.core references...", file=sys.stderr)
    root_set = scan_go_files(args.scan, args.loader)
    root_set.update(args.keep)

    if args.verbose:
        print(f"  Found {len(root_set)} direct references", file=sys.stderr)

    # Step 2: Load dependency graph and compute closure
    graph = load_dep_graph(args.graph)

    # Add all macros to root set if requested (needed for runtime .glj eval)
    if args.keep_macros:
        macro_names = {k[:-2] for k in graph.keys() if k.endswith('/M')}
        root_set.update(macro_names)
        if args.verbose:
            print(f"  Added {len(macro_names)} macros to keep set",
                  file=sys.stderr)

    if args.verbose:
        print("Computing transitive dependencies...", file=sys.stderr)
    keep_set = compute_closure(root_set, graph)

    if args.verbose:
        print(f"  Keep set: {len(keep_set)} functions", file=sys.stderr)

    # Step 3: Parse and prune loader.go
    if args.verbose:
        print("Parsing loader.go...", file=sys.stderr)
    parsed = parse_loader(args.loader)

    if args.verbose:
        print(f"  Found {len(parsed['blocks'])} function blocks",
              file=sys.stderr)

    output, kept_count = generate_pruned_loader(parsed, keep_set)

    if args.verbose:
        print(f"  Kept {kept_count} function blocks", file=sys.stderr)

    # Step 4: Write output
    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    with open(args.output, 'w') as f:
        f.write(output)

    total = len(parsed['blocks'])
    removed = total - kept_count
    orig_lines = sum(1 for _ in open(args.loader))
    new_lines = output.count('\n')

    print(f"Pruned clojure.core: {kept_count}/{total} functions kept "
          f"({removed} removed), "
          f"{new_lines}/{orig_lines} lines",
          file=sys.stderr)


if __name__ == '__main__':
    main()

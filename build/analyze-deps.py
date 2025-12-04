#!/usr/bin/env python3

import yaml
import sys

def load_graph(filename):
    """Load the dependency graph from YAML"""
    with open(filename) as f:
        return yaml.safe_load(f)

def get_all_dependencies(graph, node, visited=None):
    """Get all transitive dependencies for a node (excluding special forms)"""
    if visited is None:
        visited = set()

    if node in visited:
        return visited

    visited.add(node)

    if node in graph:
        for dep in graph[node]:
            # Skip special forms (they're primitives, not user functions)
            if not dep.endswith('/S'):
                get_all_dependencies(graph, dep, visited)

    return visited

def main():
    graph = load_graph('clojure-core.yaml')

    # Analyze println
    node = 'println'

    if node not in graph:
        print(f"'{node}' not found in dependency graph")
        # Check if it's a leaf
        all_deps = set()
        for deps in graph.values():
            all_deps.update(deps)
        if node in all_deps:
            print(f"'{node}' is a leaf node (has no dependencies)")
            return
        else:
            print(f"'{node}' not found anywhere in the graph")
            return

    print(f"Analyzing dependencies for '{node}'...\n")

    # Get all transitive dependencies
    all_deps = get_all_dependencies(graph, node)

    # Separate functions and macros
    functions = {d for d in all_deps if not d.endswith('/M')}
    macros = {d for d in all_deps if d.endswith('/M')}

    # Count special forms used
    special_forms = set()
    for dep in all_deps:
        if dep in graph:
            for subdep in graph[dep]:
                if subdep.endswith('/S'):
                    special_forms.add(subdep)

    print(f"Total unique dependencies: {len(all_deps)}")
    print(f"  Functions: {len(functions)}")
    print(f"  Macros: {len(macros)}")
    print(f"  Special forms used: {len(special_forms)}")

    print(f"\nSpecial forms required:")
    for sf in sorted(special_forms):
        print(f"  {sf}")

    if macros:
        print(f"\nMacros required ({len(macros)}):")
        for m in sorted(macros):
            print(f"  {m}")

    print(f"\nAll dependencies (sorted):")
    for i, dep in enumerate(sorted(all_deps), 1):
        # Show immediate dependencies
        if dep in graph:
            immediate = [d for d in graph[dep] if not d.endswith('/S')]
            if immediate:
                print(f"  {dep} -> {len(immediate)} deps  # {i}")
            else:
                print(f"  {dep} -> leaf  # {i}")
        else:
            print(f"  {dep} -> leaf (not in graph)  # {i}")

if __name__ == '__main__':
    main()

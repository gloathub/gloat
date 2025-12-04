#!/usr/bin/env python3

import yaml
from collections import defaultdict

def load_graph(filename):
    """Load the dependency graph from YAML"""
    with open(filename) as f:
        return yaml.safe_load(f)

def find_leaves(graph):
    """Find all leaf nodes (nodes with no dependencies)"""
    # Collect all nodes that appear as dependencies
    all_deps = set()
    for deps in graph.values():
        all_deps.update(deps)

    # Leaves are:
    # 1. All special forms (end in /S) - these are primitives
    # 2. Dependencies that never appear as keys in the graph
    leaves = set()

    for dep in all_deps:
        if dep.endswith('/S'):
            # Special forms are always leaves
            leaves.add(dep)
        elif dep not in graph:
            # Not in graph means it has no dependencies
            leaves.add(dep)

    return leaves

def count_reachable_leaves(graph, node, leaves, memo=None, visiting=None):
    """Count unique leaf nodes reachable from a given node"""
    if memo is None:
        memo = {}
    if visiting is None:
        visiting = set()

    if node in memo:
        return memo[node]

    # Detect cycles
    if node in visiting:
        return set()

    # If this node is itself a leaf
    if node in leaves:
        result = {node}
        memo[node] = result
        return result

    # If this node has no dependencies (not in graph)
    if node not in graph:
        result = {node}
        memo[node] = result
        return result

    # Mark as visiting to detect cycles
    visiting.add(node)

    # Recursively find all leaves from dependencies
    reachable = set()
    for dep in graph[node]:
        reachable.update(count_reachable_leaves(graph, dep, leaves, memo, visiting))

    # Done visiting
    visiting.remove(node)

    memo[node] = reachable
    return reachable

def main():
    print("Loading dependency graph...")
    graph = load_graph('clojure-core.yaml')

    print("Finding leaf nodes...")
    leaves = find_leaves(graph)
    print(f"Found {len(leaves)} leaf nodes")
    print(f"  Special forms: {sum(1 for l in leaves if l.endswith('/S'))}")
    print(f"  Zero-dependency functions: {sum(1 for l in leaves if not l.endswith('/S'))}")

    print("\nCounting reachable leaves for each node...")
    leaf_counts = {}
    memo = {}

    for node in graph.keys():
        reachable = count_reachable_leaves(graph, node, leaves, memo)
        leaf_counts[node] = len(reachable)

    # Also count for leaves themselves
    for leaf in leaves:
        if leaf in graph:
            reachable = count_reachable_leaves(graph, leaf, leaves, memo)
            leaf_counts[leaf] = len(reachable)
        else:
            leaf_counts[leaf] = 1  # A leaf counts itself

    print(f"\nNodes with most leaves (top 20):")
    for node, count in sorted(leaf_counts.items(), key=lambda x: -x[1])[:20]:
        print(f"  {node}: {count} leaves")

    print(f"\nNodes with fewest leaves (top 20):")
    for node, count in sorted(leaf_counts.items(), key=lambda x: x[1])[:20]:
        print(f"  {node}: {count} leaves")

    # Write results to file
    output = {}
    for node in sorted(leaf_counts.keys()):
        output[node] = leaf_counts[node]

    with open('clojure-core-leaf-counts.yaml', 'w') as f:
        yaml.dump(output, f, default_flow_style=False, sort_keys=False)

    print(f"\nWrote leaf counts to clojure-core-leaf-counts.yaml")

    # Some statistics
    counts = list(leaf_counts.values())
    print(f"\nStatistics:")
    print(f"  Total nodes analyzed: {len(leaf_counts)}")
    print(f"  Min leaves: {min(counts)}")
    print(f"  Max leaves: {max(counts)}")
    print(f"  Average leaves: {sum(counts) / len(counts):.1f}")

if __name__ == '__main__':
    main()

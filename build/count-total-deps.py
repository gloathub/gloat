#!/usr/bin/env python3

import yaml

def load_graph(filename):
    """Load the dependency graph from YAML"""
    with open(filename) as f:
        return yaml.safe_load(f)

def get_all_dependencies(graph, start_node, include_self=False):
    """Get all transitive dependencies for a node (excluding special forms)

    If include_self=True, includes the start node if it appears in its own
    dependency tree (i.e., if it's recursive).
    """
    all_deps = set()
    visiting = set()

    def traverse(node):
        # If we hit the start node again, it's recursive
        if node == start_node and node in visiting:
            all_deps.add(node)
            return

        # Already visited this node
        if node in visiting:
            return

        visiting.add(node)

        # Add this dependency (unless it's the start node and include_self=False)
        if node != start_node:
            all_deps.add(node)
        elif include_self and node in visiting:
            all_deps.add(node)

        # Traverse dependencies
        if node in graph:
            for dep in graph[node]:
                # Skip special forms
                if not dep.endswith('/S'):
                    traverse(dep)

        visiting.remove(node)

    # Start traversal
    if start_node in graph:
        for dep in graph[start_node]:
            if not dep.endswith('/S'):
                traverse(dep)

    return all_deps

def main():
    print("Loading dependency graph...")
    graph = load_graph('clojure-core.yaml')

    print("Counting total dependencies for each node...")
    dep_counts = {}

    # Count dependencies for nodes in the graph
    for node in graph.keys():
        all_deps = get_all_dependencies(graph, node, include_self=True)
        # Count all transitive dependencies (includes the node itself if recursive)
        dep_counts[node] = len(all_deps)

    # Find all leaf nodes (appear as dependencies but not in graph)
    all_referenced = set()
    for deps in graph.values():
        for dep in deps:
            if not dep.endswith('/S'):  # Skip special forms
                all_referenced.add(dep)

    # Add leaf nodes with 0 dependencies
    leaf_nodes = all_referenced - set(graph.keys())
    for leaf in leaf_nodes:
        dep_counts[leaf] = 0

    print(f"Processed {len(graph)} nodes with dependencies")
    print(f"Found {len(leaf_nodes)} leaf nodes with zero dependencies")
    print(f"Total: {len(dep_counts)} functions/macros")

    # Show some statistics
    counts = list(dep_counts.values())
    print(f"\nStatistics:")
    print(f"  Min deps: {min(counts)}")
    print(f"  Max deps: {max(counts)}")
    print(f"  Average deps: {sum(counts) / len(counts):.1f}")

    print(f"\nTop 10 nodes by total dependencies:")
    for node, count in sorted(dep_counts.items(), key=lambda x: -x[1])[:10]:
        print(f"  {node}: {count}")

    print(f"\nVerification - println: {dep_counts.get('println', 'N/A')}")

    # Write to file
    output = {k: dep_counts[k] for k in sorted(dep_counts.keys())}

    with open('clojure-core-deps-counts.yaml', 'w') as f:
        yaml.dump(output, f, default_flow_style=False, sort_keys=False)

    print(f"\nWrote total dependency counts to clojure-core-deps-counts.yaml")

if __name__ == '__main__':
    main()

gist
====

A command-line tool for creating GitHub Gists from files or stdin.

Written in [YAMLScript](https://yamlscript.org) and compiled to native
binaries with [Gloat](https://gloathub.org).

[:fontawesome-brands-github: GitHub Repository](https://github.com/ingydotnet/gist){ .md-button }
[:material-download: Releases](https://github.com/ingydotnet/gist/releases){ .md-button }


## What It Does

The `gist` command creates GitHub Gists directly from your terminal.
Pass it files, pipe in stdin, or combine both.
It prints the Gist URL to stdout.


## Key Features

- Create gists from one or more files
- Pipe content from stdin (use `-` as a filename)
- Specify a file extension for stdin (e.g. `-.md`) for proper formatting
- Flexible authentication (token file, environment variable, or interactive
  prompt)
- Single static binary with no runtime dependencies


## Usage

```bash
# Create a gist from a file
$ gist path/to/file.txt
https://gist.github.com/you/db9ad6ebfefd7016a38ef503df4f83e5

# Create a gist from stdin
$ cat file.txt | gist
https://gist.github.com/you/353c4985ea8455aff1d104291338dd9d

# Stdin with a file extension for proper formatting
$ cat notes.md | gist -.md
https://gist.github.com/you/88c4bdf058ee5a68d45fd319f8ec55d9

# Multiple files including stdin
$ cat README.md | gist file1.txt file2.txt -.md
https://gist.github.com/you/a755020d3eac8f82759232bf17b7223c
```


## Authentication

The tool requires a GitHub API token.
Provide it in any of these ways:

1. Place your token in `~/.gist-api-token`
2. Set the `GIST_API_TOKEN` environment variable
3. Set `GIST_API_TOKEN_FILE` to point to a token file
4. Enter it interactively when prompted


## Installation

### Download a release binary

Pre-built binaries are available for 13 platform targets.
Download the appropriate binary from the
[releases page](https://github.com/ingydotnet/gist/releases)
and place it in your `PATH`.

### Install with Go

```bash
go install github.com/ingydotnet/gist/go/cmd/gist@latest
```

### Build from source

```bash
git clone https://github.com/ingydotnet/gist
cd gist
make install PREFIX=~/.local
```


## Platform Releases

Every release ships binaries for 13 OS/architecture targets:

| OS | amd64 | arm64 |
|---|---|---|
| Darwin (macOS) | :white_check_mark: | :white_check_mark: |
| FreeBSD | :white_check_mark: | :white_check_mark: |
| Linux | :white_check_mark: | :white_check_mark: |
| NetBSD | :white_check_mark: | :white_check_mark: |
| OpenBSD | :white_check_mark: | :white_check_mark: |
| Windows | :white_check_mark: | :white_check_mark: |

| Target | |
|---|---|
| wasip1/wasm | :white_check_mark: |


## Built with Gloat

The `gist` tool is written in YAMLScript and compiled through Gloat's full
pipeline:

```
.ys → .clj → .glj → .go → binary
```

A single YAMLScript source file produces native binaries for all 13
targets, plus a WASM build.
The release binaries are static executables with no runtime dependencies.

# Fish completion for gloat

complete -c gloat -s h -l help -d 'Show help'
complete -c gloat -l version -d 'Show version'
complete -c gloat -l formats -d 'List available output formats'
complete -c gloat -l engines -d 'List available compilation engines'
complete -c gloat -l extensions -d 'List available processing extensions'
complete -c gloat -l platforms -d 'List available cross-compilation platforms'

complete -c gloat -s t -l to -d 'Output format' -x -a 'clj bb lg glj go dir bin lib wasm js'
complete -c gloat -s o -l out -d 'Output file or directory' -r
complete -c gloat -s E -l engine -d 'Compilation engine' -x -a 'glj graalvm lgvm lglvm lgl'
complete -c gloat -l platform -d 'Cross-compile' -x -a 'linux/amd64 linux/arm64 linux/386 linux/arm linux/ppc64le linux/s390x linux/riscv64 linux/mips64le darwin/amd64 darwin/arm64 windows/amd64 windows/arm64 windows/arm windows/386 freebsd/amd64 freebsd/arm64 freebsd/386 openbsd/amd64 openbsd/arm64 netbsd/amd64 netbsd/arm64 dragonfly/amd64 plan9/amd64 plan9/386 plan9/arm wasip1/wasm js/wasm'
complete -c gloat -s X -l ext -d 'Enable processing extension' -x -a 'brotli deps goimports gzip html open prune report serve'
complete -c gloat -l ns -d 'Override namespace' -x
complete -c gloat -l module -d 'Go module name' -x
complete -c gloat -l complete -d 'Generate shell completion' -x -a 'bash zsh fish'
complete -c gloat -l which -d 'Print path to command that gloat uses: go, glj, etc' -x

complete -c gloat -l shell -d 'Start a sub-shell with gloat tools on PATH'
complete -c gloat -l shell-all -d 'Like --shell but install all dev tools'
complete -c gloat -l repl -d 'Start REPL client (see man gloat-repl)' -x
complete -c gloat -l nrepl -d 'Start nREPL server (see man gloat-repl)' -x
complete -c gloat -l srepl -d 'Start socket REPL server (see man gloat-repl)' -x
complete -c gloat -l deps -d 'Path to gljdeps.edn (for --repl/--nrepl/--srepl)' -r -F
complete -c gloat -l classpath -d 'Classpath for REPL load paths' -x
complete -c gloat -l reset -d 'Remove all cached dependencies'
complete -c gloat -l upgrade -d 'Upgrade gloat (use --upgrade=v1.2.3 to pin a version)'
complete -c gloat -l glj-build -d 'Build the associated glj binary'

complete -c gloat -s F -l fmt -d 'Format Clojure code (GLOAT_FMT; default: zprint)'
complete -c gloat -s C -l color -d 'Syntax highlight Clojure code'
complete -c gloat -s w -l width -d 'Width for --fmt formatting' -x
complete -c gloat -s r -l run -d 'Compile and run'
complete -c gloat -s T -l time -d 'With --run, print run time to stderr'
complete -c gloat -s f -l force -d 'Overwrite existing output'
complete -c gloat -s v -l verbose -d 'Print timing information'
complete -c gloat -s q -l quiet -d 'Suppress progress messages'

# Complete source files
function __fish_complete_gloat_sources
    __fish_complete_suffix .ys
    __fish_complete_suffix .clj
    __fish_complete_suffix .glj
end

complete -c gloat -a '(__fish_complete_gloat_sources)' -d 'Source file'

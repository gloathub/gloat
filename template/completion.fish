# Fish completion for gloat

complete -c gloat -s h -l help -d 'Show help'
complete -c gloat -s v -l version -d 'Show version'
complete -c gloat -l formats -d 'List available output formats'
complete -c gloat -l extensions -d 'List available processing extensions'
complete -c gloat -l platforms -d 'List available cross-compilation platforms'

complete -c gloat -s t -l to -d 'Output format' -x -a 'clj bb glj go dir bin lib wasm js'
complete -c gloat -s o -l out -d 'Output file or directory' -r
complete -c gloat -l platform -d 'Cross-compile' -x -a 'linux/amd64 linux/arm64 darwin/amd64 darwin/arm64 windows/amd64 wasip1/wasm js/wasm'
complete -c gloat -s X -l ext -d 'Enable processing extension' -x -a 'gzip brotli'
complete -c gloat -l ns -d 'Override namespace' -x
complete -c gloat -l module -d 'Go module name' -x
complete -c gloat -l complete -d 'Generate shell completion' -x -a 'bash zsh fish'

complete -c gloat -s r -l run -d 'Compile and run'
complete -c gloat -s f -l force -d 'Overwrite existing output'
complete -c gloat -l verbose -d 'Print timing information'
complete -c gloat -s q -l quiet -d 'Suppress progress messages'

# Complete source files
function __fish_complete_gloat_sources
    __fish_complete_suffix .ys
    __fish_complete_suffix .clj
    __fish_complete_suffix .glj
end

complete -c gloat -a '(__fish_complete_gloat_sources)' -d 'Source file'

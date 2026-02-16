#compdef gloat

_gloat() {
    local -a formats platforms shells

    formats=(
        'clj:Clojure source file'
        'bb:Babashka-ready source file'
        'glj:Glojure source file'
        'go:Go source'
        'dir:Go project directory'
        'bin:Native binary'
        'lib:Shared library'
        'wasm:WebAssembly wasip1'
        'js:WebAssembly js target'
    )

    platforms=(
        'linux/amd64' 'linux/arm64' 'linux/386' 'linux/arm'
        'darwin/amd64' 'darwin/arm64'
        'windows/amd64' 'windows/arm64'
        'wasip1/wasm' 'js/wasm'
    )

    shells=('bash' 'zsh' 'fish')

    _arguments -s -S \
        '(- *)'{-h,--help}'[Show help]' \
        '(- *)'{-v,--version}'[Show version]' \
        '(- *)--formats[List available output formats]' \
        '(- *)--extensions[List available processing extensions]' \
        '(- *)--platforms[List available cross-compilation platforms]' \
        '(-t --to)'{-t,--to}'[Output format]:format:->formats' \
        '(-o --out)'{-o,--out}'[Output file or directory]:output file:_files' \
        '--platform[Cross-compile]:platform:->platforms' \
        '(-X --ext)'{-X,--ext}'[Enable processing extension]:extension:(gzip brotli)' \
        '--ns[Override namespace]:namespace:' \
        '--module[Go module name]:module:' \
        '--complete[Generate shell completion]:shell:->shells' \
        '(-r --run)'{-r,--run}'[Compile and run]' \
        '(-f --force)'{-f,--force}'[Overwrite existing output]' \
        '--verbose[Print timing information]' \
        '(-q --quiet)'{-q,--quiet}'[Suppress progress messages]' \
        '*:source file:_files -g "*.{ys,clj,glj}"'

    case "$state" in
        formats)
            _describe 'output format' formats
            ;;
        platforms)
            _describe 'platform' platforms
            ;;
        shells)
            _describe 'shell' shells
            ;;
    esac
}

_gloat "$@"

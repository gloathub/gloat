_gloat() {
    local -a formats engines platforms shells

    formats=(
        'clj:Clojure source file'
        'bb:Babashka-ready source file'
        'lg:let-go source file'
        'glj:Glojure source file'
        'go:Go source'
        'dir:Go project directory'
        'bin:Native binary'
        'lib:Shared library'
        'wasm:WebAssembly wasip1'
        'js:WebAssembly js target'
    )

    engines=(
        'glj:Glojure'
        'graalvm:GraalVM Native Image'
        'lgvm:let-go VM'
        'lglvm:let-go lower VM'
        'lgl:let-go lower'
    )

    platforms=(
        'linux/amd64' 'linux/arm64' 'linux/386' 'linux/arm'
        'linux/ppc64le' 'linux/s390x' 'linux/riscv64' 'linux/mips64le'
        'darwin/amd64' 'darwin/arm64'
        'windows/amd64' 'windows/arm64' 'windows/arm' 'windows/386'
        'freebsd/amd64' 'freebsd/arm64' 'freebsd/386'
        'openbsd/amd64' 'openbsd/arm64'
        'netbsd/amd64' 'netbsd/arm64'
        'dragonfly/amd64'
        'plan9/amd64' 'plan9/386' 'plan9/arm'
        'wasip1/wasm' 'js/wasm'
    )

    shells=('bash' 'zsh' 'fish')

    _arguments -s -S \
        '(- *)'{-h,--help}'[Show help]' \
        '(- *)--version[Show version]' \
        '(- *)--formats[List available output formats]' \
        '(- *)--engines[List available compilation engines]' \
        '(- *)--extensions[List available processing extensions]' \
        '(- *)--platforms[List available cross-compilation platforms]' \
        '(-t --to)'{-t,--to}'[Output format]:format:->formats' \
        '(-o --out)'{-o,--out}'[Output file or directory]:output file:_files' \
        '(-E --engine)'{-E,--engine}'[Compilation engine]:engine:->engines' \
        '--platform[Cross-compile]:platform:->platforms' \
        '(-X --ext)'{-X,--ext}'[Enable processing extension]:extension:(brotli deps goimports gzip html open prune report serve)' \
        '--ns[Override namespace]:namespace:' \
        '--module[Go module name]:module:' \
        '--complete[Generate shell completion]:shell:->shells' \
        '--which[Print path to command that gloat uses: go, glj, etc]:command:' \
        '(- *)--shell[Start a sub-shell with gloat tools on PATH]' \
        '(- *)--shell-all[Like --shell but install all dev tools]' \
        '--repl=[Start REPL client (see man gloat-repl)]::value:' \
        '--nrepl=[Start nREPL server (see man gloat-repl)]::value:' \
        '--srepl=[Start socket REPL server (see man gloat-repl)]::value:' \
        '--deps=[Path to gljdeps.edn (for --repl/--nrepl/--srepl)]:deps file:_files -g "*.edn"' \
        '--classpath=[Classpath for REPL load paths]:classpath:_files -/' \
        '(- *)--reset[Remove all cached dependencies]' \
        '(- *)--upgrade[Upgrade gloat (use --upgrade=v1.2.3 to pin a version)]' \
        '(- *)--glj-build[Build the associated glj binary]' \
        '(-F --fmt)'{-F,--fmt}'[Format Clojure code with zprint]' \
        '(-C --color)'{-C,--color}'[Syntax highlight Clojure code]' \
        '(-w --width)'{-w,--width}'[Width for --fmt formatting]:width:' \
        '(-r --run)'{-r,--run}'[Compile and run]' \
        '(-T --time)'{-T,--time}'[With --run, print run time to stderr]' \
        '(-f --force)'{-f,--force}'[Overwrite existing output]' \
        '(-v --verbose)'{-v,--verbose}'[Print timing information]' \
        '(-q --quiet)'{-q,--quiet}'[Suppress progress messages]' \
        '*:source file:_files -g "*.{ys,clj,glj}"'

    case "$state" in
        formats)
            _describe 'output format' formats
            ;;
        engines)
            _describe 'compilation engine' engines
            ;;
        platforms)
            _describe 'platform' platforms
            ;;
        shells)
            _describe 'shell' shells
            ;;
    esac
}

compdef _gloat gloat

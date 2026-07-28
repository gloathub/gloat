_gloat() {
    local cur prev opts formats engines platforms shells
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    opts="-h --help --version -t --to -o --out -E --engine
          -F --fmt -C --color -w --width
          -r --run -T --time -f --force -v --verbose -q --quiet
          --platform -X --ext --ns --module
          --formats --engines --extensions --platforms
          --complete --shell --shell-all
          --which --repl --nrepl --srepl --deps --classpath --reset --upgrade
          --glj-build"

    formats="clj bb lg glj go dir bin lib wasm js"
    engines="glj graalvm lgvm lglvm lgl"
    platforms="linux/amd64 linux/arm64 linux/386 linux/arm
                linux/ppc64le linux/s390x linux/riscv64 linux/mips64le
                darwin/amd64 darwin/arm64
                windows/amd64 windows/arm64 windows/arm windows/386
                freebsd/amd64 freebsd/arm64 freebsd/386
                openbsd/amd64 openbsd/arm64 netbsd/amd64 netbsd/arm64
                dragonfly/amd64 plan9/amd64 plan9/386 plan9/arm
                wasip1/wasm js/wasm"
    shells="bash zsh fish"

    case "${prev}" in
        -t|--to)
            COMPREPLY=( $(compgen -W "${formats}" -- "${cur}") )
            return 0
            ;;
        -E|--engine)
            COMPREPLY=( $(compgen -W "${engines}" -- "${cur}") )
            return 0
            ;;
        --platform)
            COMPREPLY=( $(compgen -W "${platforms}" -- "${cur}") )
            return 0
            ;;
        -X|--ext)
            COMPREPLY=( $(compgen -W "brotli deps goimports gzip html open prune report serve" -- "${cur}") )
            return 0
            ;;
        --complete)
            COMPREPLY=( $(compgen -W "${shells}" -- "${cur}") )
            return 0
            ;;
        -o|--out)
            COMPREPLY=( $(compgen -f -- "${cur}") )
            return 0
            ;;
        --repl)
            COMPREPLY=( $(compgen -d -- "${cur}") )
            return 0
            ;;
        --deps)
            COMPREPLY=( $(compgen -f -X '!*.edn' -- "${cur}") )
            return 0
            ;;
        --classpath)
            COMPREPLY=( $(compgen -d -- "${cur}") )
            return 0
            ;;
        -w|--width|--ns|--module|--which)
            # No completion for free-form text
            return 0
            ;;
    esac

    if [[ ${cur} == -* ]] ; then
        COMPREPLY=( $(compgen -W "${opts}" -- "${cur}") )
        return 0
    fi

    # Complete source files (.ys, .clj, .glj) and directories
    COMPREPLY=( $(compgen -f -X '!*.ys' -- "${cur}")
                $(compgen -f -X '!*.clj' -- "${cur}")
                $(compgen -f -X '!*.glj' -- "${cur}")
                $(compgen -d -- "${cur}") )
}

complete -o filenames -F _gloat gloat

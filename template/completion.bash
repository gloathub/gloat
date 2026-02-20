_gloat() {
    local cur prev opts formats platforms shells
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    opts="-h --help -v --version -t --to -o --out -r --run -f --force
          --verbose -q --quiet --platform -X --ext --ns --module
          --formats --extensions --platforms --complete"

    formats="clj bb glj go dir bin lib wasm js"
    platforms="linux/amd64 linux/arm64 darwin/amd64 darwin/arm64
                windows/amd64 wasip1/wasm js/wasm"
    shells="bash zsh fish"

    case "${prev}" in
        -t|--to)
            COMPREPLY=( $(compgen -W "${formats}" -- "${cur}") )
            return 0
            ;;
        --platform)
            COMPREPLY=( $(compgen -W "${platforms}" -- "${cur}") )
            return 0
            ;;
        -X|--ext)
            COMPREPLY=( $(compgen -W "gzip brotli" -- "${cur}") )
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
        --ns|--module)
            # No completion for free-form text
            return 0
            ;;
    esac

    if [[ ${cur} == -* ]] ; then
        COMPREPLY=( $(compgen -W "${opts}" -- "${cur}") )
        return 0
    fi

    # Complete source files (.ys, .clj, .glj) and directories
    COMPREPLY=( $(compgen -f -X '!*.@(ys|clj|glj)' -- "${cur}")
                $(compgen -d -- "${cur}") )
}

complete -o filenames -F _gloat gloat

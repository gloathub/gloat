# getopt-long library based around `git rev-parse --parseopt`

getopt() {
  [[ ${getopt_spec-} ]] ||
    getopt_spec=$(cat)

  [[ $getopt_spec ]] ||
    getopt:error "No getopt 'spec' provided"

  getopt:set-vars

  getopt:parse-spec "$getopt_spec"

  [[ $# -gt 0 ]] ||
    set -- "${getopt_default[@]}"

  if [[ ${#getopt_cmds[*]} -gt 0 ]]; then
    local words=()
    while [[ $# -gt 0 ]]; do
      if [[ " ${getopt_cmds[*]} " == *" $1 "* ]]; then
        cmd=$1
        shift
        words+=( -- "$@" )
        break
      else
        words+=( "$1" )
        shift
      fi
    done
    set -- "${words[@]}"
  fi

  local output
  local rc=0
  output=$(
    git rev-parse --parseopt --stuck-long -- "$@" <<<"$getopt_parseopt"
  ) || rc=$?

  if [[ $rc -ne 0 ]]; then
    if [[ $output == cat* ]]; then
      local pager=${PAGER-}
      [[ $pager && $pager != less ]] || pager='less -FRX'
      eval "$output" |
        perl -pe 's/--\[no-\]/--/g' |
        $pager
    elif [[ $output ]]; then
      die "Unexpected results from 'git rev-parse --parseopt'"
    fi
    exit $rc
  else
    eval "$output"
  fi

  getopt:set-opts "$@"

  if [[ $cmd && $getopt_cmds_spec ]]; then
    getopt:cmd-getopt "${args[@]}"
  else
    local debug_var=$getopt_prefix$getopt_debug
    if [[ ${!debug_var-} == true ]]; then
      set -x
    fi
  fi
}

getopt:cmd-getopt() {
  [[ $(type -t "$getopt_cmds_spec") == function ]] ||
    getopt:error \
      "getopt_cmds_spec='$getopt_cmds_spec' must be a function name"
  local getopt_spec=''
  local getopt_default=()
  local getopt_cmds=()
  local getopt_cmds_find=''
  getopt_spec=$("$getopt_cmds_spec" "$cmd")

  if [[ $getopt_spec ]]; then
    local getopt_cmds_spec=''
    local cmd=''
    getopt "$@"
  fi
}

getopt:set-vars() {
  getopt_parseopt=''
  getopt_required=()
  getopt_database=''

  [[ ${getopt_cmds[*]+x} ]] || getopt_cmds=()
  [[ ${getopt_cmds_find+x} ]] || getopt_cmds_find=''
  [[ ${getopt_cmds_spec+x} ]] || getopt_cmds_spec=''

  cmd=''
  args=()
}

getopt:reset() {
  unset getopt_spec
  getopt_default=()
  getopt_debug=debug
  getopt_prefix=option_
  getopt_args=args

  cmd=''
  getopt_cmds=()
  getopt_cmds_find=''
  getopt_cmds_spec=''
}

getopt:source() {
  if [[ $# -eq 0 ]]; then
    getopt:error "Use '--' (source getopt.bash --) if no args for source."
  fi
  getopt:reset
  for arg; do
    if [[ $arg == -- ]]; then
      break
    elif [[ $arg == *$'\n--\n'* ]]; then
      getopt_spec=$arg
    else
      getopt:error "Invalid source arg '$arg'. Use '--' if no args."
    fi
  done
}

# Read through the provided 'getopt_spec' to collect the various instructions.
# Save all the instructions in a structured format in 'getopt_database'.
# Create a new spec 'getopt_parseopt' that is compliant with rev-parse's needs.
getopt:parse-spec() {
  local option_section=false
  local top=true
  local var count

  while IFS=$'\n' read -r line; do
    if [[ $line == \#\ * || $line == \# ]]; then
      continue
    fi

    # Look for getopt config variables at top of spec:
    if $top; then
      if [[ $line == getopt_* ]]; then
        eval "$line"
        continue
      elif [[ $line == '' ]]; then
        top=false
        continue
      else
        top=false
      fi
    fi

    # Check for start of options section:
    if ! $option_section; then
      if [[ $line == -- ]]; then
        option_section=true
      fi
      if [[ $getopt_cmds_find == true && $line == \ * ]]; then
        local cmd rest
        read -r cmd rest <<<"$line"
        getopt_cmds+=("$cmd")
      elif [[ $getopt_cmds_find && $line =~ $getopt_cmds_find ]]; then
        local match
        for match in "${BASH_REMATCH[@]}"; do
          if [[ $match =~ ^[-a-z0-9]+$ ]]; then
            getopt_cmds+=("$match")
          fi
        done
      fi
      getopt_parseopt+=$line$'\n'
      continue
    fi

    # Change an empty line to ' ' so it will appear in error messages:
    if [[ $line == '' ]]; then
      getopt_parseopt+=$' \n'
      continue
    fi

    # if 's,long...' (short,long)
    if [[ $line =~ ^([a-zA-Z0-9]),([a-z][-a-z0-9]+)([=?*][^\ ]*)?\  ]]; then
      short=${BASH_REMATCH[1]}
      long=${BASH_REMATCH[2]}
      kind=${BASH_REMATCH[3]:0:1}
      flag=${BASH_REMATCH[3]:1}
      name=$long

    # if 's...' (short only)
    elif [[ $line =~ ^([a-zA-Z0-9])([=?*][^\ ]*)?\  ]]; then
      short=${BASH_REMATCH[1]}
      long=''
      kind=${BASH_REMATCH[2]}
      flag=${BASH_REMATCH[2]:1}
      name=$short

    # if 'long...' (long only)
    elif [[ $line =~ ^([a-z][-a-z0-9]+)([=?*][^\ ]*)?\  ]]; then
      short=''
      long=${BASH_REMATCH[1]}
      kind=${BASH_REMATCH[2]}
      flag=${BASH_REMATCH[2]:1}
      name=$long

    else
      getopt:error "Bad line in getopt spec '$line'"
    fi

    var=$getopt_prefix${long:-$short}
    count=${getopt_prefix}count_${long:-$short}
    var=${var//-/_}
    count=${count//-/_}

    printf -v "$count" 0

    if [[ $kind == '='* ]]; then
      kind=value
      if ! [[ ${!var-} ]]; then
        # XXX Is there a better way to indirectly assign array variable =('')?
        if shopt -s compat41 2>/dev/null; then
          declare -g -a "$var"=''
        else
          eval "$var=('')"
        fi
      fi
    elif [[ $kind == '?'* ]]; then
      kind=dual
      printf -v "$var" ''
    else
      kind=bool
      if [[ ${!var-} != true ]]; then
        printf -v "$var" false
      fi
    fi

    type='' mult=false

    orig=flag
    while [[ $flag ]]; do
      if [[ ${flag:0:1} == '+' ]]; then
        getopt_required+=("$name")
        flag=${flag:1}
        line=${line/+/}
      elif [[ ${flag:0:1} == '*' ]]; then
        flag=${flag:1}
      elif [[ ${flag:0:1} == '@' ]] && ! $mult; then
        mult=true
        flag=${flag:1}
        line=${line/@/}
      elif [[ $flag =~ ^(str|num|file|dir|path) ]] ||
        [[ $flag =~ ^([0-9]+\.\.([0-9]+)?) ]]
      then
        type=${BASH_REMATCH[1]}
        flag=${flag:${#type}}
        line=${line/$type\ /\ }
      else
        getopt:error "Can't parse getopt flags '$orig' at '$flag'"
        flag=''
        exit 1
      fi
    done

    getopt_parseopt+=$line$'\n'

    getopt_database+="$name ${short:-_} ${long:=_} $kind ${type:-_} $mult"$'\n'
  done <<<"$1"
}

getopt:set-opts() {
  local option name var
  local need=" ${getopt_required[*]}"
  local _name short long kind type mult

  option-vars() {
    name=${1#-}
    name=${name#-}
    unset value
    if [[ $name == *=* ]]; then
      value=${name#*=}
      name=${name%%=*}
    fi
    spec=$(grep "^$name " <<<"$getopt_database") ||
      die "Can't grep '^$name ' in:" "$getopt_database"
    read -r _name short long kind type mult <<<"$spec"
    long=${long#_}
    short=${short#_}
    show=${long:+"--$long=…"}
    show=${show:-"-$short …"}
  }

  while [[ $# -gt 0 ]]; do
    option=$1; shift

    if [[ $option == -- ]]; then
      if [[ ${need##\ } ]]; then
        for option in $need; do
          option-vars "$option"
          echo "* Option '$show' is required" >&2
        done
        getopt:error 'Missing required options'
      fi

      # TODO use getopt_args here
      args=("$@")
      return
    fi

    option-vars "$option"

    need=${need/\ $name/}

    var=$getopt_prefix${long:-$short}
    count=${getopt_prefix}count_${long:-$short}
    var=${var//-/_}
    count=${count//-/_}

    # Set bool option variable to 'true':
    if [[ $kind == bool ]]; then
      printf -v "$var" true

    # Push value onto option array:
    elif [[ $kind == value ]]; then
      [[ $type ]] && getopt:validate
      IFS=$'\n' read -r -d '' "${var}[${!count}]" <<<"$value" || true

    elif [[ $kind == dual ]]; then
      if [[ ${value-} ]]; then
        [[ $type ]] && getopt:validate
        if ! [[ ${!var} ]]; then
          printf -v "$var" '%s' "$value"
        fi
      else
        printf -v "$var" true
      fi
    else
      die
    fi

    # Increment option_count_<name>
    # shellcheck disable=2004
    : $(( ${count} = ${!count} + 1 ))
  done

  [[ $# -eq 0 ]] ||
    getopt:error "Unexpected arguments: '$*'"
}

getopt:validate() (
  case "$type" in
    _) ;;
    str) ;;
    file) [[ -f "$value" ]] ||
      getopt:error "$(getopt:msg) file does not exist";;
    dir) [[ -d "$value" ]] ||
      getopt:error "$(getopt:msg) directory does not exist";;
    path) [[ -e "$value" ]] ||
      getopt:error "$(getopt:msg) path does not exist";;
    *)
      if [[ $type == num ]] &&
         [[ ! $value =~ ^-?[0-9]+$ ]]
      then
        getopt:error "$(getopt:msg) is not a number"
      elif [[ $type =~ ^([0-9]+)\.\.([0-9]+)$ ]]; then
        lower=${BASH_REMATCH[1]}
        upper=${BASH_REMATCH[2]}
        if [[ ! $value =~ ^-?[0-9]+$ ]] ||
           [[ $value -lt $lower ]] ||
           [[ $value -gt $upper ]]
        then
          getopt:error "$(getopt:msg) is not in the range"
        fi
      else
        getopt:error "Validation type '$type' not yet supported"
      fi
      ;;
  esac
)

getopt:msg() (
  echo "$option is type '$type', but value '$value'"
)

getopt:error() {
  die "[getopt] Error: $1"
}

[[ $0 == "${BASH_SOURCE[0]}" ]] ||
  getopt:source "$@"

export -f getopt

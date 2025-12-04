# test-tap.bash - Complete TAP test framework for Bash
#
# Copyright (c) 2013-2022. Ingy döt Net.

test-tap:main() {
  _test_tap__call_stack_level=1

  [[ ${BASH_SOURCE[0]} ]] ||
    test-tap:die "Error: test-tap-bash must be run under Bash only"
  _test_tap__plan=0
  _test_tap__run=0
  _test_tap__failed=0
  _test_tap__pid=${BASHPID:-0}

  if [[ $# -gt 0 ]]; then
    if ! [[ $# -eq 1 && $1 == -- ]]; then
      if [[ $# -eq 2 && $1 == tests && $2 =~ ^[0-9]+$ ]]; then
        plan "$@"
      else
        test-tap:die 'Usage: source test-tap.bash tests <number>'
      fi
    fi
  fi

  trap test-tap:end EXIT
}

test-tap:label() {
  local label G W
  label=$1
  G=${got-}
  W=${want-}
  G=${G:0:40}
  W=${W:0:40}
  G=${G//$'\n'/␤}
  W=${W//$'\n'/␤}

  label=${label//\%G/$G}
  label=${label//\%W/$W}

  echo -e "$label"
}

test-tap:end() {
  local rc=$?
  test-tap:_check-pid
  if [[ $rc -ne 0 ]]; then
    if [[ ${_test_tap__bail_msg-} ]] ||
       [[ ${test_tap_bail_on_fail-} ]]; then
      local bail=${_test_tap__bail_msg:-"Bailing out on status=$rc"}
      echo "Bail out!  $bail"
      exit $rc
    fi
  fi

  if [[ $_test_tap__plan -eq 0 ]]; then
    if [[ $_test_tap__run -gt 0 ]]; then
      echo "# Tests were run but no plan was declared." >&2
    fi
  else
    if [[ $_test_tap__run -eq 0 ]]; then
      echo "# No tests run!" >&2
    elif [[ $_test_tap__run -ne $_test_tap__plan ]]; then
      local msg="# Looks like you planned $_test_tap__plan tests but ran $_test_tap__run."
      [[ $_test_tap__plan -eq 1 ]] && msg=${msg/tests/test}
      echo "$msg" >&2
    fi
  fi

  local exit_code=0
  if [[ $_test_tap__failed -gt 0 ]]; then
    exit_code=$_test_tap__failed
    [[ $exit_code -gt 254 ]] && exit_code=254
    local msg="# Looks like you failed $_test_tap__failed tests of $_test_tap__run run."
    [[ $_test_tap__failed -eq 1 ]] && msg=${msg/tests/test}
    echo "$msg" >&2
  fi

  exit $exit_code
}

test-tap:_check-pid() {
  if [[ ${BASHPID:-0} -ne ${_test_tap__pid:-0} ]]; then
    test-tap:die "Error: Called test-tap method from a subprocess" 3
  fi
}

test-tap:is-fail() {
  local _test_tap__call_stack_level=
  _test_tap__call_stack_level=$(( _test_tap__call_stack_level + 1 ))
  if [[ $want =~ $'\n' ]]; then
    echo "$got" > /tmp/got-$$
    echo "$want" > /tmp/want-$$
    diff -u /tmp/{want,got}-$$ >&2 || true
    wc /tmp/{want,got}-$$ >&2
    rm -f /tmp/{got,want}-$$
  else
    diag "\
    got: '$got'
  expected: '$want'"
  fi
}

test-tap:isnt-fail() {
    diag "\
      got: '$got'
   expected: anything else"
}

test-tap:like-fail() {
    diag "Got: '$got'"
}

test-tap:unlike-fail() {
    diag "Got: '$got'"
}

test-tap:die() {
  echo "$1" >&2
  exit 1
}

#------------------------------------------------------------------------------
# API Functions (in order of documentation)
#------------------------------------------------------------------------------
plan() {
  test-tap:_check-pid
  if [[ $# -eq 2 && $1 == skip-all ]]; then
    printf "1..0 # SKIP %s\n" "$2"
    exit 0
  fi

  if [[ $# -eq 2 && $1 == tests ]]; then
    shift
  fi

  if [[ $# -eq 1 ]]; then
    [[ $1 =~ ^-?[0-9]+$ ]] ||
      test-tap:die 'Plan must be a number'
    [[ $1 -gt 0 ]] ||
      test-tap:die 'Plan must be greater then 0'
    _test_tap__plan=$1
    printf "1..%d\n" "$_test_tap__plan"
  else
    test-tap:die 'Usage: plan <number>'
  fi
}

skip-all() {
  plan skip-all "$@"
}

done-testing() {
  test-tap:_check-pid
  _test_tap__plan=$_test_tap__run
  echo 1.."${1:-$_test_tap__run}"
}

bail-on-fail() {
  test-tap:_check-pid
  test_tap_bail_on_fail=1
}

try() {
  cmd="$*"
  set --
  set +e
  got=$(eval "$cmd" 2>&1)
  rc=$?
  set -e
}

pass() {
  test-tap:_check-pid
  echo -e "ok $((++_test_tap__run))${1:+" - $1"}"
}

fail() {
  test-tap:_check-pid
  ((++_test_tap__run))
  IFS=' ' read -r -a c <<<"$(caller "$_test_tap__call_stack_level")"
  local file=${c[2]-}
  local line=${c[0]-}
  local label=${1-} callback=${2-}
  echo -e "not ok $_test_tap__run${label:+" - $label"}"
  label=${label:+"'$label'\n#   at $file line $line."}
  label=${label:-"at $file line $line."}
  echo -e "#   Failed test $label" >&2

  [[ $callback ]] && $callback

  local rc=${test_tap_bail_on_fail:-0}
  [[ $rc -eq 0 ]] || exit "$rc"
}

ok() {
  if (exit "${1:-$?}"); then
    pass "${2-}"
  else
    fail "${2-}"
  fi
}

not-ok() {
  if (exit "${1:-$?}"); then
    fail "${2-}"
  else
    pass "${2-}"
  fi
}

is() {
  local got=$1 want=$2 label=${3-}
  label=$(test-tap:label "$label")
  if [[ $got == "$want" ]]; then
    pass "$label"
  else
    fail "$label" test-tap:is-fail
  fi
}

isnt() {
  local _test_tap__call_stack_level=
  _test_tap__call_stack_level=$(( _test_tap__call_stack_level + 1 ))
  local got=$1 want=$2 dontwant=$2 label=${3-}
  label=$(test-tap:label "$label")
  if [[ $got != "$dontwant" ]]; then
    pass "$label"
  else
    fail "$label" test-tap:isnt-fail
  fi
}

like() {
  local got=$1 want=$2 regex=$2 label=${3-}
  label=$(test-tap:label "$label")
  if [[ $got =~ $regex ]]; then
    pass "$label"
  else
    fail "$label" test-tap:like-fail
  fi
}

unlike() {
  local got=$1 want=$2 regex=$2 label=${3-}
  label=$(test-tap:label "$label")
  if [[ ! $got =~ $regex ]]; then
    pass "$label"
  else
    fail "$label" test-tap:unlike-fail
  fi
}

has() {
  local got=$1 want=$2 label=${3-}
  label=$(test-tap:label "$label")
  local n=$'\n'

  if [[ $got == *"$want"* ]]; then
    pass "$label"
  else
    fail "$label"
    if [[ $got == *"$n"* ]]; then
      got="'$n$got$n'"
    else
      got="'$got'"
    fi
    diag "Text '$want' NOT found in: $got"
  fi
}

hasnt() {
  local got=$1 want=$2 label=${3-}
  label=$(test-tap:label "$label")
  local n=$'\n'

  if [[ $got != *"$want"* ]]; then
    pass "$label"
  else
    fail "$label"
    if [[ $got == *"$n"* ]]; then
      got="'$n$got$n'"
    else
      got="'$got'"
    fi
    diag "Text '$want' WAS found in: $got"
  fi
}

array:is() {
  local arrayname="$1[@]"
  local expname="$2[@]"
  local label=${3-}

  local array=("${!arrayname}")
  local expected=("${!expname}")

  is "$(printf "%s\n" "${array[@]}")" \
    "$(printf "%s\n" "${expected[@]}")" \
    "$label"
}

diag() {
  test-tap:_check-pid
  local msg=$*
  msg="# ${msg//$'\n'/$'\n'\# }"
  echo -e "$msg" >&2
}

note() {
  test-tap:_check-pid
  local msg=$*
  msg="# ${msg//$'\n'/$'\n'\# }"
  echo -e "$msg"
}

bail-out() {
  test-tap:_check-pid
  _test_tap__bail_msg=$*
  : "${_test_tap__bail_msg:=No reason given.}"
  exit 255
}


#------------------------------------------------------------------------------
# File System Testing Functions
#------------------------------------------------------------------------------
ok-d() {
  local dir=${1:?}
  local msg=${2:-"'${dir#*//}' is a directory"}
  ok "$([[ -d $dir ]])" "$msg"
}

ok-e() {
  local path=${1:?}
  local msg=${2:-"'${path#*//}' exists"}
  ok "$([[ -e $path ]])" "$msg"
}

ok-empty() {
  local path=${1:?}
  local msg
  if [[ -f $path ]]; then
    msg=${2:-"'${path#*//}' is an empty file"}
    ok "$([[ ! -s $path ]])" "$msg"
  elif [[ -d $path ]]; then
    msg=${2:-"'${path#*//}' is an empty directory"}
    ok "$(shopt -s nullglob; ! [[ $(echo "$path"/*) ]])" "$msg"
  else
    fail "'${path#*//}' does not exist"
  fi
}

ok-not-e() {
  local path=${1:?}
  local msg=${2:-"'${path#*//}' does not exist"}
  ok "$([[ ! -e $path ]])" "$msg"
}

ok-f() {
  local file=${1:?}
  local msg=${2:-"'${file#*//}' is a file"}
  ok "$([[ -f $file ]])" "$msg"
}

ok-h() {
  local link=${1:?}
  local msg=${2:-"'${link#*//}' is a symlink"}
  ok "$([[ -h $link ]])" "$msg"
}

ok-s() {
  local file=${1:?}
  local msg=${2:-"'${file#*//}' is a non-empty file"}
  ok "$([[ -s $file ]])" "$msg"
}

ok-w() {
  local path=${1:?}
  local msg=${2:-"'${path#*//}' is writable"}
  ok "$([[ -w $path ]])" "$msg"
}

ok-x() {
  local path=${1:?}
  local msg=${2:-"'${path#*//}' is executable"}
  ok "$([[ -x $path ]])" "$msg"
}

test-tap:main "$@"

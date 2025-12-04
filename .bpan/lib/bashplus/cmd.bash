# Check if a name is a command.
+cmd:ok() [[ $(command -v "${1:?+cmd:ok requires a command name}") ]]

# Functions to assert that commands are available.
+cmd:assert() {
  +is-cmd "$@" ||
    error "Command '$1' is required"
}
+cmd:assert-ver() {
  +cmd:ok-ver "$@" ||
    error "Command '$1' version '$2' or higher is required"
}

# Assert stable versions of heavily used commands:
+cmd:assert-perl() { +cmd:assert-ver perl ${1:-5.10.1}; }
+cmd:assert-git()  { +cmd:assert-ver git  ${1:-2.9}; }

# Check if command exists and is at or above a version.
+cmd:ok-ver() {
  local command=$1 version=$2
  local out got want

  out=$("$command" --version 2>/dev/null) ||
    return

  [[ $out =~ ([0-9]+\.[0-9]+(\.[0-9]+)?) ]] ||
    die "Can't determine version number from '$command'"

  IFS=. read -r -a got <<< "${BASH_REMATCH[1]}"
  : "${got[2]:=0}"

  IFS=. read -r -a want <<<"$version"
  : "${want[0]:=0}"
  : "${want[1]:=0}"
  : "${want[2]:=0}"

  (( got[0] > want[0] ||
    (( got[0] == want[0] &&
      (( got[1] > want[1] ||
        (( got[1] == want[1] && got[2] >= want[2] ))
      ))
    ))
  ))
}

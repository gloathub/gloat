# This is the BPAN Bash bootstrap library.
# It sets up a sane Bash program runtime environment with minimal code.

bpan:version() ( echo '0.1.107' )

# 'die' is so common we define a very basic one here.
# The 'bashplus' library defines a more full featured one.
die() { printf '%s\n' "$@" >&2; exit 1; }

# Assert that Bash in version 3.2 or higher:
( shopt -s compat31 2>/dev/null ) ||
  die "'bpan.bash' requires Bash version 3.2 or higher"

# Put startup code in a function:
bpan:main() {
  # Add local .bpan/lib to BPAN_PATH
  local dir
  dir=$(cd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")/.." && pwd -P)
  [[ $dir == "${BPAN_ROOT-}" ]] ||
    dir=${dir%/.bpan}
  dir+=/.bpan/lib
  [[ -d $dir && ${BPAN_PATH-} != *"$dir"* ]] &&
    BPAN_PATH=${BPAN_PATH:+$BPAN_PATH:}$dir

  # Settings to make Bash as strict as possible:
  {
    set -o errexit
    set -o nounset
    set -o pipefail
    shopt -s inherit_errexit
  } 2>/dev/null || true
  # -u works poorly with arrays until Bash 4.4
  ( shopt -s compat43 2>/dev/null ) || set +o nounset

  # 'source bpan.bash ...' can take arguments:
  local arg
  for arg do
    # shellcheck disable=2018,2019
    case "$arg" in
      # No arguments intended.
      # This is needed to ignore global '$@'.
      --) break;;

      # Set the 'app', 'App', and 'APP' variables:
      --app)
        app=$(basename "$0")
        app=${app%.*}
        App=$(tr a-z A-Z <<<"${app:0:1}")${app:1}
        APP=$(tr a-z A-Z <<<"$app")
        ;;

      *) die "Unknown argument '$arg' for '${BASH_SOURCE[0]}'";;
    esac
  done
}

# Try to source a bash library in $BPAN_PATH:
bpan:source() {
  local set=$-; set "${BPAN_X:-+x}"

  [[ $# -gt 0 ]] ||
    die "Usage: bpan:source <bpan-library-name> [<arg>...]"

  local name=$1; shift

  local dir
  # shellcheck disable=2086
  for dir in $(IFS=:; echo ${BPAN_PATH-}); do
    if [[ -f $dir/$name.bash ]]; then
      source "$dir/$name.bash" "$@"
      [[ $set != *x* ]] || set -x
      return
    fi
  done

  die "Unable to 'bpan:source $name'"
}

bpan:main "$@"

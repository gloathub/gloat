# Source this file from Bash to use the local development checkouts.

if [[ ${BASH_SOURCE[0]} == "$0" ]]; then
  printf '%s\n' 'Source this file instead of executing it:' >&2
  printf '%s\n' '  source test-env.bash' >&2
  exit 1
fi

_test_env_root=$(
  cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P
)
_test_env_m2=$_test_env_root/repos/yamlscript/.cache/.local/home/.m2/repository
_test_env_gitlibs=$_test_env_root/.cache/test-env/gitlibs

_test_env_prepend_path() {
  case :$PATH: in
    *:"$1":*) ;;
    *) PATH=$1:$PATH ;;
  esac
}

_test_env_prepend_path "$_test_env_root/repos/yamlschema/bin"
_test_env_prepend_path "$_test_env_root/repos/jolt/bin"
_test_env_prepend_path "$_test_env_root/repos/gobb/bin"
_test_env_prepend_path "$_test_env_root/repos/glojure/bin/linux_amd64"
_test_env_prepend_path "$_test_env_root/repos/yamlscript/ys/bin"
_test_env_prepend_path "$_test_env_root/bin"
export PATH

export GLOAT_ROOT=$_test_env_root
export GLOAT_YS=$_test_env_root/repos/yamlscript/ys/bin/ys
export GLOAT_JOLT=$_test_env_root/repos/jolt/bin/jolt
export GLOJURE_DIR=$_test_env_root/repos/glojure
export GOBB_GLOAT=$_test_env_root/bin/gloat

export YS_MAVEN_REPOSITORY=$_test_env_m2
export GLOJURE_MAVEN_REPOSITORY=$_test_env_m2
export GOBB_MAVEN_REPOSITORY=$_test_env_m2
export JOLT_MAVEN_REPOSITORY=$_test_env_m2
export GRENADINE_MAVEN_REPOSITORY=$_test_env_m2

export YS_GITLIBS_DIR=$_test_env_gitlibs
export GLOJURE_GITLIBS_DIR=$_test_env_gitlibs
export GOBB_GITLIBS_DIR=$_test_env_gitlibs
export JOLT_GITLIBS_DIR=$_test_env_gitlibs
export GRENADINE_GITLIBS_DIR=$_test_env_gitlibs

unset -f _test_env_prepend_path
unset _test_env_root _test_env_m2 _test_env_gitlibs

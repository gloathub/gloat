#!/usr/bin/env bash

source test/init

+cmd:ok-ver shellcheck 0.11.0 ||
  plan skip-all "Test requires shellcheck 0.11.0 to be installed"

skip=1064,1072,1073,1090,1091,2002,2030,2031,2034,2154,2207,2217

while read -r file; do
  [[ -h $file ]] && continue
  [[ -f $file ]] || continue

  shebang=$(head -n1 "$file" | tr -d '\0')

  if [[ $file == *.bash ]] ||
     [[ $shebang == '#!'*[/\ ]bash ]]
  then
    ok "$(shellcheck -e "$skip" "$file")" \
      "Bash file '$file' passes shellcheck"

  elif
    [[ $file == *.sh ]] ||
    [[ $shebang == '#!'*[/\ ]sh ]]
  then
    ok "$(shellcheck -e "$skip" "$file")" \
      "Shell script file '$file' passes shellcheck"
  fi
done < <(
  git ls-files |
    grep -v '^share/template'
)

done-testing

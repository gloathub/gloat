# Transform config.yaml format to config.json format
# Input: {program, args, bottles: [[[label,val,sel?],...]], ...}
# Output: {program, args, bottles: {options:[{label,value},...], selected:X}}

def transform_options:
  .[0] |
  if (.[0] | type) == "array" then
    # Explicit format: [[label, value, selected?], ...]
    {
      options: [.[] | {label: .[0], value: .[1]}],
      selected: ([.[] | select(.[2] == true) | .[1]] | first // .[0][1])
    }
  else
    # Simple format: [val1, val2, [selected], ...]
    {
      options: [.[] | if type == "array" then .[0] else . end |
                {label: tostring, value: .}],
      selected: ([.[] | select(type == "array") | .[0]] | first // .[0])
    }
  end;

. as $root |
.program as $prog |
($root[$prog] // $root[$prog + ".ys"] // $root["src/" + $prog] // $root["src/" + $prog + ".ys"]) as $match |
if $match then
  . + {($prog): $match | transform_options}
else
  .
end

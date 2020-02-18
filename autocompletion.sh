# Usage
# First setup an alias for workflow
# In your .bash_profile file in your home folder
# Add an alias for workflow
# e.g. alias workflow = java -jar [path to workflow jar]
# below that line add
# source [path to this file]
# This will enable autocompletion when using workflow on the command line

function autocomplete {
  local currentWord workflows options
  COMPREPLY=()

  currentWord="${COMP_WORDS[COMP_CWORD]}"
  # word index is less than two, auto complete list is workflows.
  if [ $COMP_CWORD -lt 2 ]; then
    if [ -z "$CACHED_WORKFLOWS" ]; then
      export CACHED_WORKFLOWS="$(workflow GenerateAutoCompleteValues --log-level=WARN)"
    fi
    options=${CACHED_WORKFLOWS}
  else
    # autocomplete list is arguments for selected workflow
    local workflowToRun=${COMP_WORDS[1]}
    # no cached values set
    if [ -z "$CACHED_WORKFLOW" ]; then
      export CACHED_WORKFLOW=${workflowToRun}
      export CACHED_WORKFLOW_ARGUMENTS="$(workflow GenerateAutoCompleteValues --autocomplete-workflow=${workflowToRun} --log-level=WARN)"
    fi

    # cached workflow is different, reset values
    if [ "${workflowToRun}" != "${CACHED_WORKFLOW}" ]; then
      export CACHED_WORKFLOW=${workflowToRun}
      export CACHED_WORKFLOW_ARGUMENTS="$(workflow GenerateAutoCompleteValues --autocomplete-workflow=${workflowToRun} --log-level=WARN)"
    fi
    options=${CACHED_WORKFLOW_ARGUMENTS}
  fi
  COMPREPLY=( $(compgen -W "${options}" -- ${currentWord}) )
  return 0
}

# enables autocomplete for workflow alias
complete -o nospace -F autocomplete workflow

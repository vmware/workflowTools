function autoCompleteWorkflow {
  local current workflows
  COMPREPLY=()
  cur="${COMP_WORDS[COMP_CWORD]}"

  local workflowToRun=${COMP_WORDS[0]}
  if [ -z "$CACHED_WORKFLOW" ]; then
    export CACHED_WORKFLOW=${workflowToRun}
    export CACHED_WORKFLOW_VALUES="$(WORKFLOW_ALIAS GenerateAutoCompleteValues --autocomplete-workflow=${workflowToRun} --log-level=WARN)"
  fi

  if [ "${workflowToRun}" != "${CACHED_WORKFLOW}" ]; then
    export CACHED_WORKFLOW=${workflowToRun}
    export CACHED_WORKFLOW_VALUES="$(WORKFLOW_ALIAS GenerateAutoCompleteValues --autocomplete-workflow=${workflowToRun} --log-level=WARN)"
  fi

  COMPREPLY=( $(compgen -W "${CACHED_WORKFLOW_VALUES}" -- ${cur}) )
  return 0
}
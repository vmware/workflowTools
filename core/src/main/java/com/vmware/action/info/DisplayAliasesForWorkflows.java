package com.vmware.action.info;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Set;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.ClasspathResource;
import com.vmware.util.IOUtils;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Displays a list of command line aliases for workflow actions, can also save output to a file. Supports auto completion for aliases")
public class DisplayAliasesForWorkflows extends BaseAction {
    public DisplayAliasesForWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Displaying aliases for workflows, including autocomplete {}", commandLineConfig.includeAliasAutocomplete);
        Set<String> workflows = config.workflows.keySet();
        List<String> supportingWorkflows = config.supportingWorkflows;
        workflows.removeAll(supportingWorkflows);

        try (BufferedWriter writer = outputWriter()) {
            outputAliasLines(workflows, writer);

            if (commandLineConfig.includeAliasAutocomplete) {

                outputAutoCompleteSection(workflows, writer);
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private void outputAliasLines(Set<String> workflows, BufferedWriter writer) throws IOException {
        for (String workflow : workflows) {
            writer.write(String.format("alias %s='%s %s'", workflow, commandLineConfig.workflowAlias, workflow));
            writer.newLine();
        }
    }

    private void outputAutoCompleteSection(Set<String> workflows, BufferedWriter writer) throws IOException {
        Reader reader = new ClasspathResource("/workflowCompletion.sh", this.getClass()).getReader();
        String functionText = IOUtils.read(reader, true, LogLevel.DEBUG);
        functionText = functionText.replace("WORKFLOW_ALIAS", commandLineConfig.workflowAlias);

        writer.newLine();
        writer.write(functionText);
        writer.newLine();

        for (String workflow : workflows) {
            writer.write(String.format("complete -o nospace -F autoCompleteWorkflow %s", workflow));
            writer.newLine();
        }
    }
}

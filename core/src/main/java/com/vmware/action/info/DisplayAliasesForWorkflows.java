package com.vmware.action.info;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;

@ActionDescription("Displays a list of bash aliases for workflow actions, can also save output to a file.")
public class DisplayAliasesForWorkflows extends BaseAction {
    public DisplayAliasesForWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Displaying aliases for workflows");
        Set<String> workflows = config.workflows.keySet();
        List<String> supportingWorkflows = config.supportingWorkflows;
        workflows.removeAll(supportingWorkflows);

        try (BufferedWriter writer = outputWriter()) {
            for (String workflow : workflows) {
                try {
                    writer.write(String.format("alias %s='workflow %s'", workflow, workflow));
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}

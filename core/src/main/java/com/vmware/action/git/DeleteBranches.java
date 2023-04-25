package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;

import java.util.Map;

@ActionDescription("Iterate through all local branches and offer option to delete")
public class DeleteBranches extends BaseAction {
    public DeleteBranches(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("git");
    }

    @Override
    public void process() {
        Map<String, String> branches = git.allBranches();
        for (String branchName : branches.keySet()) {
            String deleteBranch = InputUtils.readValueUntilNotBlank(String.format("Delete %s: %s (y/n)", branchName, branches.get(branchName)));
            if ("y".equalsIgnoreCase(deleteBranch)) {
                git.deleteBranch(branchName);
            }
        }
    }
}

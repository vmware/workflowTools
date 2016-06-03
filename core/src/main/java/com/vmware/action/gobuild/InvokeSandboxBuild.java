package com.vmware.action.gobuild;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

import java.util.logging.Level;

@ActionDescription("Used to invoke a sandbox build")
public class InvokeSandboxBuild extends BaseCommitAction {

    public InvokeSandboxBuild(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String changelistId = draft.matchingChangelistId;
        if (StringUtils.isBlank(changelistId)) {
            changelistId = InputUtils.readValueUntilNotBlank("Changelist id for sandbox");
        }

        String[] inputs = new String[] {"beta\n", "\n", "\n", "\n"};
        String[] textsToWaitFor = new String[] {"Buildtype to use [beta]:", "top of this baseline)", "vcloud?", "queued (vcloud/sp-main)"};
        String command = config.goBuildBinPath + " sandbox queue vcloud --branch=sp-main --changeset=" + changelistId;

        CommandLineUtils.executeScript(command, inputs, textsToWaitFor, Level.INFO);
    }

}

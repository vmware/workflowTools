package com.vmware.action.gobuild;

import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

import java.util.logging.Level;

import static java.lang.String.format;

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

        String[] inputs = new String[] {"beta", "", "", ""};
        String[] textsToWaitFor = new String[] {
                "Buildtype to use [beta]:", "top of this baseline)", config.buildwebProject + "?",
                format("queued (%s/%s)", config.buildwebProject, config.buildwebBranch)};
        String command = format("%s sandbox queue %s --branch=%s --changeset=%s",
                config.goBuildBinPath, config.buildwebProject, config.buildwebBranch, changelistId);

        String output = CommandLineUtils.executeScript(command, inputs, textsToWaitFor, Level.INFO);
        String buildWebPattern = config.getCommitConfiguration().generateBuildwebUrlPattern();

        String buildUrl = MatcherUtils.singleMatch(output, buildWebPattern);
        if (buildUrl != null) {
            log.info("Adding build {} to commit", buildUrl);
            draft.jobBuilds.add(new JobBuild(buildUrl, BuildResult.BUILDING));
        } else {
            log.warn("Unable to parse build url using pattern {}", buildWebPattern);
        }


    }

}

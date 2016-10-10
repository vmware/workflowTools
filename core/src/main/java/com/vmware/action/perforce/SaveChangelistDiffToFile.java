package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;
import java.util.List;

@ActionDescription("Saves a git compatible diff of the specified changelist to a file.")
public class SaveChangelistDiffToFile extends BasePerforceCommitAction {
    public SaveChangelistDiffToFile(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String changelistIdToUse = determineChangelistIdToUse();
        String outputFilePath = StringUtils.isNotBlank(config.outputFileForContent) ? config.outputFileForContent
                : "changelist" + changelistIdToUse + ".patch";
        String content = perforce.diffChangelistInGitFormat(changelistIdToUse, true, LogLevel.TRACE);
        File outputFile = new File(outputFilePath);
        outputFile.delete();
        log.info("Saving diff for changelist {} to file {}", changelistIdToUse, outputFile.getPath());
        FileUtils.saveToFile(outputFile, content);
    }

    private String determineChangelistIdToUse() {
        if (StringUtils.isNotBlank(draft.perforceChangelistId)) {
            return draft.perforceChangelistId;
        } else if (StringUtils.isNotBlank(config.changelistId)) {
            return config.changelistId;
        } else {
            List<String> openChangelists = perforce.getPendingChangelists();
            if (openChangelists.isEmpty()) {
                throw new IllegalArgumentException("No pending changelists in client " + config.perforceClientName);
            }
            if (openChangelists.size() == 1) {
                String changelistToUse = openChangelists.get(0);
                log.info("Using changelist {} as it is the only pending changelist in client {}",
                        changelistToUse, config.perforceClientName);
                return openChangelists.get(0);
            } else {
                int selection = InputUtils.readSelection(openChangelists, "Select changelist as source for diff");
                return openChangelists.get(selection);
            }
        }
    }
}

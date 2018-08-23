package com.vmware.action.patch;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;

@ActionDescription("Saves diff data to a file.")
public class SaveDiffToFile extends BasePerforceCommitAction {
    public SaveDiffToFile(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isBlank(draft.draftPatchData)) {
            return "no patch data to save";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        String changelistIdToUse = determineChangelistIdToUse();
        String outputFilePath = StringUtils.isNotBlank(patchConfig.outputFileForContent) ? patchConfig.outputFileForContent
                : "workflowPatch.patch";
        String content = perforce.diffChangelistInGitFormat(changelistIdToUse, LogLevel.TRACE);
        File outputFile = new File(outputFilePath);
        outputFile.delete();
        log.info("Saving diff to file {}", outputFile.getPath());
        FileUtils.saveToFile(outputFile, content);
    }

}

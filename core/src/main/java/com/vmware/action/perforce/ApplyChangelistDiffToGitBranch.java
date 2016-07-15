package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.scm.FileChange;
import com.vmware.scm.FileChangeType;
import com.vmware.util.FileUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.input.InputUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

@ActionDescription("Applies a diff for the selected changelist to the current git branch. Can be used to apply shelved changelists.")
public class ApplyChangelistDiffToGitBranch extends BasePerforceCommitAction {

    public ApplyChangelistDiffToGitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String changelistIdToUse = draft.perforceChangelistId;
        if (StringUtils.isBlank(changelistIdToUse)) {
            List<String> changelists = perforce.getPendingChangelists();
            int selection = InputUtils.readSelection(changelists.toArray(new String[changelists.size()]), "Select changelist");
            changelistIdToUse = changelists.get(selection);
        }

        log.info("Generating git compatible diff for perforce changelist {}", changelistIdToUse);
        String diffData = perforce.diffChangelistInGitFormat(changelistIdToUse, true);

        String checkOutput = git.applyDiff(diffData, true);
        if (StringUtils.isNotBlank(checkOutput)) {
            log.debug("Failed diff\n{}" + diffData);
            throw new IllegalArgumentException("Check of git diff failed!\n" + checkOutput);
        }
        log.info("Check of diff succeeded, applying diff");
        String applyOutput = git.applyDiff(diffData, false);
        if (StringUtils.isNotBlank(applyOutput)) {
            saveDiffAndThrowException(applyOutput, diffData);
        }
        log.info("Successfully applied diff");
    }

    private void saveDiffAndThrowException(String applyOutput, String diffData) {
        try {
            File tempPatchFile = File.createTempFile("failedDiff", ".patch");
            FileUtils.saveToFile(tempPatchFile, diffData);
            throw new IllegalArgumentException("Failed to apply diff cleanly, saved diff to temp file "
                    + tempPatchFile.getPath() + "!\n" + applyOutput);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}

package com.vmware.action.patch;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionAfterFailedPatchCheck;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.RepoType;
import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.Perforce;
import com.vmware.util.scm.diff.DiffConverter;
import com.vmware.util.scm.diff.GitDiffToPerforceConverter;
import com.vmware.util.scm.diff.PerforceDiffToGitConverter;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;

import java.io.File;
import java.util.List;

@ActionDescription("Used to apply patch data.")
public class ApplyPatch extends BaseCommitAction {

    public ApplyPatch(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        super.failIfTrue(StringUtils.isEmpty(draft.draftPatchData), "no patch data loaded");
    }

    @Override
    public void process() {
        String patchData = draft.draftPatchData;
        RepoType repoType = draft.repoType;

        List<FileChange> fileChanges = null;
        boolean isPerforceClient = !git.workingDirectoryIsInGitRepo();
        if (isPerforceClient) {
            exitIfPerforceClientCannotBeUsed();
        }

        DiffConverter diffConverter = null;
        if (repoType == RepoType.perforce) {
            diffConverter = new PerforceDiffToGitConverter(git);
            patchData = diffConverter.convert(patchData);
        } else if (isPerforceClient){
            diffConverter = new GitDiffToPerforceConverter(getLoggedInPerforceClient(), "");
            diffConverter.convert(patchData); // used to generate file changes
        }

        if (diffConverter != null) {
            fileChanges = diffConverter.getFileChanges();
        }

        String changelistId = null;
        if (isPerforceClient) {
            changelistId = determineChangelistIdToUse();
            Perforce perforce = getLoggedInPerforceClient();
            perforce.syncPerforceFiles(fileChanges, "");
            perforce.openFilesForEditIfNeeded(changelistId, fileChanges);
        }
        log.trace("Patch to apply\n{}", patchData);

        File patchFile = new File(System.getProperty("user.dir") + File.separator + "workflow.patch");
        patchFile.delete();
        IOUtils.write(patchFile, patchData);

        PatchCheckResult patchCheckResult = checkIfPatchApplies(patchFile);
        String result = applyPatch(patchFile, patchCheckResult);

        if (StringUtils.isEmpty(result.trim())) {
            log.info("Patch successfully applied, patch is stored in workflow.patch");
        } else {
            printPatchResult(result);
        }
        if (isPerforceClient) {
            serviceLocator.getPerforce().renameAddOrDeleteFiles(changelistId, fileChanges);
        }
    }

    protected String applyPatch(File patchFile, PatchCheckResult patchCheckResult) {
        log.info("Applying patch");
        String result = "";
        switch (patchCheckResult) {
            case applyPatchUsingGitApply:
                result = git.applyPatchFile(patchFile, false);
                break;
            case applyPartialPatchUsingGitApply:
                result = git.applyPartialPatchFile(patchFile);
                break;
            case applyPartialPatchUsingPatchCommand:
            case applyPatchUsingPatchCommand:
                result = patch(patchFile, false);
                break;
            default:
                cancelWithWarnMessage("Not applying patch, patch is stored in workflow.patch");
         }
        return result;
    }

    private void exitIfPerforceClientCannotBeUsed() {
        String perforceClientCannotBeUsed = perforceClientCannotBeUsed();
        if (perforceClientCannotBeUsed != null) {
            throw new RuntimeException(perforceClientCannotBeUsed);
        }
    }

    private PatchCheckResult checkIfPatchApplies(File patchFile) {
        log.info("Checking if patch applies");
        String checkResult = patchConfig.usePatchCommand ? patch(patchFile, true) : git.applyPatchFile(patchFile, true);
        String checkCommand = patchConfig.usePatchCommand ? patchConfig.patchCommand + " --dry-run"
                : "git apply --ignore-whitespace -3 --check";
        if (StringUtils.isEmpty(checkResult)) {
            return patchConfig.usePatchCommand ? PatchCheckResult.applyPatchUsingPatchCommand :
                    PatchCheckResult.applyPatchUsingGitApply;
        }

        printCheckOutput(checkResult, checkCommand);
        patchConfig.actionAfterFailedPatchCheck = ActionAfterFailedPatchCheck.askForAction(patchConfig.usePatchCommand);

        switch (patchConfig.actionAfterFailedPatchCheck) {
            case partialWithGit:
                return PatchCheckResult.applyPartialPatchUsingGitApply;
            case partialWithPatch:
                return PatchCheckResult.applyPartialPatchUsingPatchCommand;
            case usePatchCommand:
                patchConfig.usePatchCommand = true;
                return checkIfPatchApplies(patchFile);
            default:
                return PatchCheckResult.nothing;
        }
    }

    private void printCheckOutput(String output, String title) {
        Padder padder = new Padder(title);
        padder.errorTitle();
        log.info(output);
        padder.errorTitle();
        log.error("Checking of patch failed!");
    }

    private void printPatchResult(String result) {
        log.warn("Potential issues with applying patch, patch is stored in workflow.patch");
        Padder padder = new Padder("Patch Output");
        padder.warnTitle();
        log.info(result);
        padder.warnTitle();
    }

    private String patch(File patchFile, boolean dryRun) {
        String command = patchConfig.patchCommand;
        if (dryRun) {
            command += " --dry-run";
        }
        LogLevel logLevel = dryRun ? LogLevel.DEBUG : LogLevel.INFO;
        return CommandLineUtils.executeCommand(null, command, IOUtils.read(patchFile) + "\n", logLevel);
    }

    private enum PatchCheckResult {
        nothing,
        applyPartialPatchUsingGitApply,
        applyPartialPatchUsingPatchCommand,
        applyPatchUsingGitApply,
        applyPatchUsingPatchCommand;
    }
}

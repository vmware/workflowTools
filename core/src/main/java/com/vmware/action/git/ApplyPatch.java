package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.scm.FileChange;
import com.vmware.scm.Perforce;
import com.vmware.scm.diff.GitDiffToPerforceConverter;
import com.vmware.scm.diff.PerforceDiffToGitConverter;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;

import java.io.File;
import java.util.List;

@ActionDescription("Used to apply diff data as a patch")
public class ApplyPatch extends BaseCommitAction {

    public ApplyPatch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String diffData = draft.draftDiffData;
        String repoType = draft.repoType;

        List<FileChange> fileChanges = null;
        boolean isPerforceClient = !git.workingDirectoryIsInGitRepo();
        if (repoType.toLowerCase().contains("perforce")) {
            PerforceDiffToGitConverter diffConverter = new PerforceDiffToGitConverter();
            diffData = diffConverter.convert(diffData, git);
            fileChanges = diffConverter.getFileChanges();
        } else if (isPerforceClient){
            Perforce perforce = serviceLocator.getPerforce();
            GitDiffToPerforceConverter diffConverter = new GitDiffToPerforceConverter(perforce, "");
            diffConverter.convert(diffData);
            fileChanges = diffConverter.getFileChanges();
        }

        String changelistId = null;
        if (isPerforceClient) {
            changelistId = determineChangelistIdToUse();
            Perforce perforce = serviceLocator.getPerforce();
            perforce.syncPerforceFiles(fileChanges, "");
            perforce.openFilesForEditIfNeeded(changelistId, fileChanges);
        }
        log.trace("Patch to apply\n{}", diffData);

        File patchFile = new File(System.getProperty("user.dir") + File.separator + "workflow.patch");
        patchFile.delete();
        IOUtils.write(patchFile, diffData);

        checkIfPatchApplies(diffData);

        log.info("Applying patch");
        String result = config.usePatchToApplyDiff ? patchDiff(diffData, false) : git.applyDiff(diffData, false);

        if (StringUtils.isBlank(result.trim())) {
            log.info("Diff successfully applied, patch is stored in workflow.patch");
            if (isPerforceClient) {
                serviceLocator.getPerforce().renameAddOrDeleteFiles(changelistId, fileChanges);
            }
        } else {
            printDiffResult(result);
        }
    }

    protected void checkIfPatchApplies(String diffData) {
        log.info("Checking if patch applies");
        String checkResult = config.usePatchToApplyDiff ? patchDiff(diffData, true) : git.applyDiff(diffData, true);
        if (StringUtils.isNotBlank(checkResult) && !config.usePatchToApplyDiff) {
            printCheckOutput(checkResult, "git apply --ignore-whitespace -3 --check");
            String usePatchCommand = InputUtils.readValueUntilNotBlank("Use patch command [" + config.patchCommand + "] to apply patch (Y/N)?");
            config.usePatchToApplyDiff = "y".equalsIgnoreCase(usePatchCommand);
            if (!config.usePatchToApplyDiff) {
                log.warn("Not applying patch, patch is stored in workflow.patch");
                System.exit(0);
            } else {
                checkIfPatchApplies(diffData);
            }
        } else if (StringUtils.isNotBlank(checkResult)){
            printCheckOutput(checkResult, config.patchCommand + " --dry-run");
            String applyPatch = InputUtils.readValue("Do you still want to apply the patch (Y/N)?");
            if (!applyPatch.equalsIgnoreCase("y")) {
                log.warn("Not applying patch, patch is stored in workflow.patch");
                System.exit(0);
            }
        }
    }

    private void printCheckOutput(String output, String title) {
        Padder padder = new Padder(title);
        padder.errorTitle();
        log.info(output);
        padder.errorTitle();
        log.error("Checking of patch failed!");
    }

    private void printDiffResult(String result) {
        log.warn("Potential issues with applying diff, patch is stored in workflow.patch");
        Padder padder = new Padder("Patch Output");
        padder.warnTitle();
        log.info(result);
        padder.warnTitle();
    }

    private String patchDiff(String diffData, boolean dryRun) {
        String command = config.patchCommand;
        if (dryRun) {
            command += " --dry-run";
        }
        LogLevel logLevel = dryRun ? LogLevel.DEBUG : LogLevel.INFO;
        return CommandLineUtils.executeCommand(null, command, diffData, logLevel);
    }
}

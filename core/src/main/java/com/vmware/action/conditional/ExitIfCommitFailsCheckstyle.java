package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.scm.FileChange;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static com.vmware.scm.FileChangeType.deleted;
import static com.vmware.scm.FileChangeType.deletedAfterRename;
import static com.vmware.util.StringUtils.appendWithDelimiter;

@ActionDescription("Runs checkstyle against files in commit. Does not run if config values are not set")
public class ExitIfCommitFailsCheckstyle extends BaseCommitAction {

    private static final String CHECKSTYLE_COMMAND_FORMAT = "java -Dcheckstyle.suppressions.file=%s -jar %s -c %s %s";

    public ExitIfCommitFailsCheckstyle(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isBlank(config.checkstyleJarPath)) {
            return "checkstyleJarPath is not set";
        } else if (StringUtils.isBlank(config.checkstyleConfigXmlPath)) {
            return "checkstyleConfigXmlPath is not set";
        } else if (StringUtils.isBlank(config.checkstyleSuppressionsXmlPath)) {
            return "checkstyleSuppressionsXmlPath is not set";
        } else {
            return super.cannotRunAction();
        }
    }

    @Override
    public void process() {
        log.debug("Using checkstyle jar {} with config file {} and suppressions file {}",
                config.checkstyleJarPath, config.checkstyleConfigXmlPath, config.checkstyleSuppressionsXmlPath);
        String checkstyleJarFullPath = createFullPath(config.checkstyleJarPath);
        String checkstyleConfigXmlFullPath = createFullPath(config.checkstyleConfigXmlPath);
        String checkstyleSuppressionsXmlFullPath = createFullPath(config.checkstyleSuppressionsXmlPath);

        log.debug("Using checkstyle jar path {} with config file path {} and suppressions file path {}",
                checkstyleJarFullPath, checkstyleConfigXmlFullPath, checkstyleSuppressionsXmlFullPath);

        List<FileChange> fileChanges = getFileChangesInLastCommit();

        List<String> filePathsToCheck = fileChanges.stream().filter(fileChange -> fileChange.matchesNoneOf(deleted, deletedAfterRename))
                .filter(this::fileChangeStartsWithMapping)
                .map(fileChange -> createFullPath(fileChange.getLastFileAffected())).collect(Collectors.toList());
        String fileCheckText = "";
        for (String filePath : filePathsToCheck) {
            fileCheckText = appendWithDelimiter(fileCheckText, filePath, " ");
        }
        if (StringUtils.isBlank(fileCheckText)) {
            log.info("No files need to be checked for checkstyle errors");
            return;
        }

        String checkstyleCommand = String.format(CHECKSTYLE_COMMAND_FORMAT,
                checkstyleSuppressionsXmlFullPath, checkstyleJarFullPath, checkstyleConfigXmlFullPath, fileCheckText);
        log.debug("Executing checkstyle command {}", checkstyleCommand);
        String checkstyleOutput = CommandLineUtils.executeCommand(checkstyleCommand, LogLevel.DEBUG);
        String errorCount = MatcherUtils.singleMatch(checkstyleOutput, "Checkstyle ends with (\\d+) errors.");
        if (errorCount != null) {
            String plural = Integer.parseInt(errorCount) == 1 ? "" : "s";
            String title = String.format("%s checkstyle error%s encountered", errorCount, plural);
            Padder errorPadder = new Padder(title);
            errorPadder.errorTitle();
            log.info(checkstyleOutput);
            errorPadder.errorTitle();
            System.exit(0);
        } else {
            log.info("All files passed checkstyle check");
            log.debug(checkstyleOutput);
        }
    }

    private boolean fileChangeStartsWithMapping(FileChange fileChange) {
        if (!fileChange.getLastFileAffected().endsWith(".java")) {
            return false;
        }
        if (config.checkstyleFileMappings == null || config.checkstyleFileMappings.isEmpty()) {
            return true;
        }
        return config.checkstyleFileMappings.stream()
                .anyMatch(fileMapping -> fileChange.getLastFileAffected().startsWith(fileMapping));
    }

    private String createFullPath(String relativePath) {
        File relativeFile = new File(relativePath);
        if (relativeFile.exists()) {
            return relativePath;
        }
        String fullPath;
        if (git.workingDirectoryIsInGitRepo()) {
            fullPath = git.fullPath(relativePath);
        } else if (perforceClientCannotBeUsed() == null) {
            fullPath = getLoggedInPerforceClient().fullPath(relativePath);
        } else {
            throw new FatalException("File path " + relativePath + " does not exist");
        }
        if (!new File(fullPath).exists()) {
            throw new FatalException("File path " + fullPath + " does not exist");
        }
        return fullPath;
    }
}

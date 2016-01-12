package com.vmware;

import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.Padder;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class around the git command line command.
 * Exposes git functionality needed for workflows.
 */
public class Git {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private File workingDirectory = null;
    private Boolean gitInstalled;

    public Git() {
    }

    public Git(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * @return The root directory for this repo. Null if this is not a repo
     */
    public File getRootDirectory() {
        FileFilter gitDirectoryFilter = new GitDirectoryFilter();
        File matchingDirectory = workingDirectory != null ? workingDirectory : new File(System.getProperty("user.dir"));
        while (matchingDirectory != null && matchingDirectory.listFiles(gitDirectoryFilter).length != 1) {
            matchingDirectory = matchingDirectory.getParentFile();
        }
        return matchingDirectory;
    }

    public String applyDiff(String diffData) {
        return executeGitCommand("git apply -3 --index", diffData, false);
    }

    public String lastCommitText(boolean prettyPrint) {
        return commitText(0, prettyPrint);
    }

    public String commitText(int skipCount, boolean prettyPrint) {
        String prettyPrintCommand = prettyPrint ? " --pretty=%B\n" : "";
        return executeGitCommand("git log -1 --skip=" + skipCount + prettyPrintCommand);
    }

    public String currentBranch() {
        return executeGitCommand("git rev-parse --abbrev-ref HEAD");
    }

    public String configValue(String propertyName) {
        if (!isGitInstalled()) {
            log.debug("Returning empty string for git config value {} as git is not installed", propertyName);
            return "";
        }
        return executeGitCommand("git config " + propertyName);
    }

    public int totalCommitCount() {
        String commitCount = executeGitCommand("git rev-list HEAD --count");
        return Integer.parseInt(commitCount);
    }

    public Map<String, String> configValues() {
        if (!isGitInstalled()) {
            log.debug("Returning empty maps for git config values as git is not installed");
            return new HashMap<String, String>();
        }

        String configText = executeGitCommand("git config -l");
        String[] valuesAsText = configText.split("[\r\n]+");
        Map<String, String> values = new HashMap<String, String>();
        for (String valueAsText : valuesAsText) {
            String[] valuePieces = valueAsText.split("=");
            if (valuePieces.length == 2) {
                values.put(valuePieces[0], valuePieces[1]);
            } else {
                log.debug("{} git config value could not be parsed", valueAsText);
            }
        }
        return values;
    }

    public void commit(String msg) {
        executeCommitCommand("git commit", msg);
    }

    public void commitWithAllFileChanges(String msg) {
        executeCommitCommand("git commit --all", msg);
    }

    public void amendCommit(String msg) {
        executeCommitCommand("git commit --amend", msg);
    }

    public void amendCommitWithAllFileChanges(String msg) {
        executeCommitCommand("git commit --amend --all", msg);
    }

    public byte[] diff(String parentRef, String commitRef, boolean supportsRenames) {
        String renamesFlag = supportsRenames ? "-M " : "--no-renames ";
        String diffCommand = "git diff %s--no-color --full-index --no-ext-diff --ignore-submodules %s..%s";
        diffCommand = String.format(diffCommand, renamesFlag, parentRef, commitRef);
        byte[] diffOutput = executeGitCommand(diffCommand).getBytes();
        return diffOutput.length > 0 ? diffOutput : null;
    }

    public byte[] diff(String commitRef, boolean supportsRenames) {
        String renamesFlag = supportsRenames ? "-M " : "--no-renames ";
        String diffCommand = "git diff " + renamesFlag + "--no-color --full-index --no-ext-diff --ignore-submodules " + commitRef;
        return executeGitCommand(diffCommand).getBytes();
    }

    public void push() {
        String trackingBranch = getTrackingBranch();

        if (trackingBranch == null) {
            log.error("No tracking branch for current branch {}", currentBranch());
            System.exit(1);
        }

        String remoteBranch = trackingBranch.substring("origin/".length());
        pushToRemoteBranch(remoteBranch);
    }

    public void pushToRemoteBranch(String remoteBranch) {
        pushToRemoteBranch(remoteBranch, false);
    }

    public void pushToRemoteBranch(String remoteBranch, boolean forceUpdate) {
        String currentHeadRef = revParse("HEAD");
        log.info("Pushing commit {} to {}", currentHeadRef, remoteBranch);

        String forceUpdateString = forceUpdate ? " -f" : "";
        String pushCommand = String.format("git push origin head:%s%s --porcelain", remoteBranch, forceUpdateString);

        String pushOutput = executeGitCommand(pushCommand, true);

        if (pushOutput.contains("[up to date]")) {
            log.info("Remote branch is already up to date");
            return;
        } else if (pushOutput.contains("[new branch]")) {
            return;
        } else if (pushOutput.contains("[rejected]")) {
            log.error("Git push was rejected!");
            System.exit(1);
        }

        String updatedRemoteHeadRef = MatcherUtils.singleMatch(pushOutput, "\\w\\.\\.\\.*(\\w+)");
        if (updatedRemoteHeadRef == null) {
            log.error("Could not parse updated remote branch ref from push output, assuming failure");
            System.exit(1);
        }

        if (!currentHeadRef.startsWith(updatedRemoteHeadRef)) {
            log.error("Git push failed, remote branch ref of {} does not match local head ref",
                    updatedRemoteHeadRef, currentHeadRef);
            System.exit(1);
        }
        log.info("Remote branch was successfully updated");
    }

    public String mergeBase(String upstreamBranch, String commitRef) {
        return executeGitCommand("git merge-base " + upstreamBranch + " " + commitRef);
    }

    public String revParse(String commitRef) {
        return executeGitCommand("git rev-parse " + commitRef);
    }

    public String getTrackingBranch() {
        String branchName = currentBranch();
        String headRef = revParse("HEAD");

        String branchOutput = executeGitCommand("git branch -vv");

        Matcher trackingBranchMatcher = Pattern.compile(branchName + "\\s*(\\w+)\\s*\\[(.+?)\\]").matcher(branchOutput);
        if (!trackingBranchMatcher.find()) {
            return null;
        }

        String refFragment = trackingBranchMatcher.group(1);
        if (!headRef.startsWith(refFragment)) {
            return null;
        }

        String trackingBranch = trackingBranchMatcher.group(2);
        int colonIndex = trackingBranch.indexOf(":");
        if (colonIndex != -1) {
            trackingBranch = trackingBranch.substring(0, colonIndex);
        }

        log.debug("Parsed tracking branch {} from git branch -vv", trackingBranch);
        return trackingBranch;
    }

    public String initRepo() {
        return executeGitCommand("git init");
    }

    public String addAllFiles() {
        return executeGitCommand("git add --all");
    }

    public String reset(String ref) {
        return executeGitCommand("git reset --hard " + ref);
    }

    public List<String> getStagedChanges() {
        return getChanges(false);
    }

    public List<String> getAllChanges() {
        return getChanges(true);
    }

    private List<String> getChanges(boolean includeUnStagedChanges) {
        List<String> changes = new ArrayList<String>();
        String gitStatusOutput = executeGitCommand("git status --porcelain" );

        String pattern = String.format("^(\\s*)(%s+)\\s+(.+)", FileChange.allValuesPattern());
        Matcher changesMatcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(gitStatusOutput);

        while (changesMatcher.find()) {
            String leadingWhitespace = changesMatcher.group(1);
            String changesText = changesMatcher.group(2);
            String filePath = changesMatcher.group(3);

            for (char changeLetter : changesText.toCharArray()) {
                FileChange fileChange = FileChange.valueOf(String.valueOf(changeLetter));
                // leading whitespace means that the change is unStaged
                if (includeUnStagedChanges || leadingWhitespace.isEmpty()) {
                    int arrowIndex = filePath.indexOf("->");
                    String filePathToUse = filePath;
                    // if you modify a renamed file, then the output will be RM old -> new
                    // for the modify action we only want to show the new file name
                    if (fileChange != FileChange.R && arrowIndex != -1) {
                        filePathToUse = filePath.substring(arrowIndex + 3);
                    }

                    changes.add(fileChange.getLabel() + " " + filePathToUse);
                }
            }
        }

        return Collections.unmodifiableList(changes);
    }

    private boolean isGitInstalled() {
        if (gitInstalled != null) {
            return gitInstalled;
        }

        String osName = System.getProperty("os.name");
        log.debug("Os name {}", osName);
        if (osName == null) {
            gitInstalled = false;
        } else if (osName.startsWith("Windows")) {
            String gitWhereCheck = executeCommand("where git", null, false);
            log.debug("{} git where check [{}]", osName, gitWhereCheck);
            gitInstalled = !gitWhereCheck.contains("Could not find files");
        } else {
            String gitWhichCheck = executeCommand("which git", null, false);
            log.debug("{} git which check [{}]", osName, gitWhichCheck);
            gitInstalled = !gitWhichCheck.trim().isEmpty();
        }
        return gitInstalled;
    }

    private void executeCommitCommand(String commitCommand, String msg) {
        executeGitCommand(commitCommand + " --file=-", msg, false);
    }

    private String executeGitCommand(String gitCommand) {
        return executeGitCommand(gitCommand, false);
    }

    private String executeGitCommand(String gitCommand, boolean printLines) {
        return executeGitCommand(gitCommand, null, printLines);
    }

    private String executeGitCommand(String gitCommand, String inputText, boolean printLines) {
        log.debug("Git command {}", gitCommand);
        String gitOutput = executeCommand(gitCommand, inputText, printLines);
        exitIfGitCommandFailed(gitOutput);
        return gitOutput;
    }

    private String executeCommand(String command, String inputText, boolean printLines) {
        ProcessBuilder builder = new ProcessBuilder(command.split(" ")).directory(workingDirectory)
                .redirectErrorStream(true);
        try {
            Process statusProcess = builder.start();
            if (inputText != null) {
                IOUtils.write(statusProcess.getOutputStream(), inputText);
            }
            return readProcessOutput(statusProcess.getInputStream(), printLines);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private String readProcessOutput(InputStream input, boolean printLines) {
        Padder titlePadder = new Padder("Git Output");
        Level logLevel = printLines ? Level.INFO : Level.FINEST;

        titlePadder.logTitle(logLevel);
        String output = IOUtils.read(input);
        if (printLines) {
            log.info(output);
        } else {
            log.trace(output);
        }
        titlePadder.logTitle(logLevel);
        return output;
    }

    private void exitIfGitCommandFailed(String gitOutput) {
        if (StringUtils.isBlank(gitOutput)) {
            return;
        }

        if (gitOutput.trim().startsWith("fatal: Not a git repository")) {
            log.error("{} is not in a git repository", System.getProperty("user.dir"));
            System.exit(1);
        }
    }

    public enum FileChange {
        A("added"),
        M("modified"),
        D("deleted"),
        R("renamed"),
        C("copied");
        private String label;

        private FileChange(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static String allValuesPattern() {
            String pattern = "[";

            for (FileChange fileChange : FileChange.values()) {
                pattern += fileChange.name();

            }
            pattern += "]";
            return pattern;
        }
    }

    private class GitDirectoryFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && file.getName().equals(".git");
        }
    }

}

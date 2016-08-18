package com.vmware.scm;

import com.vmware.util.FileUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.util.CommandLineUtils.isCommandAvailable;

/**
 * Wrapper class around the git command line command.
 * Exposes git functionality needed for workflows.
 */
public class Git extends BaseScmWrapper {
    private Boolean gitInstalled;

    public Git() {
        super(ScmType.git);
        super.setWorkingDirectory(new File(System.getProperty("user.dir")));
    }

    public Git(File workingDirectory) {
        super(ScmType.git);
        super.setWorkingDirectory(workingDirectory);
    }

    /**
     * @return The root directory for this repo. Null if this is not a repo
     */
    public File getRootDirectory() {
        FileFilter gitDirectoryFilter = new GitDirectoryFilter();
        File matchingDirectory = workingDirectory;
        while (matchingDirectory != null && matchingDirectory.listFiles(gitDirectoryFilter).length != 1) {
            matchingDirectory = matchingDirectory.getParentFile();
        }
        return matchingDirectory;
    }

    public String newTrackingBranch(String branchName, String trackingBranch) {
        return executeScmCommand("git checkout -b {} {}", branchName, trackingBranch);
    }

    public String applyDiff(String diffData, boolean check) {
        exitIfNotInRepoRootFolder("git apply must be run from the root folder " + getRootDirectory().getPath() + ", don't know why");
        String checkString = check ? " --check" : "";
        return executeScmCommand("git apply -3 {}", diffData, LogLevel.DEBUG, checkString);
    }

    public String diffTree(String fromRef, String ref, boolean binaryPatch) {
        String binaryFlag = binaryPatch ? " --binary" : "";
        return executeScmCommand("git diff-tree -M{} --full-index -U3 -p {} {}", binaryFlag, fromRef, ref) + "\n";
    }

    public String applyDiffToPerforce(String rootDirectory, String diffData, boolean check) {
        exitIfNotInRepoRootFolder("git apply must be run from the root folder " + getRootDirectory().getPath() + ", don't know why");
        String checkString = check ? " --check" : "";
        String output = executeScmCommand("git apply --directory={}{}", diffData, LogLevel.DEBUG, rootDirectory, checkString);
        if (StringUtils.isBlank(output)) {
            return output;
        }

        try {
            File tempFile = File.createTempFile("invalidDiff", ".patch");
            log.warn("Diff did not apply cleanly, saving diff to temp File {}", tempFile.getPath());
            FileUtils.saveToFile(tempFile, diffData);
        } catch (IOException e) {
            log.warn("Failed to create temp file to save invalid diff patch to: " + e.getMessage(), e);
        }
        return output;
    }

    public String[] lastSubmittedChangelistInfo() {
        Pattern gitP4Pattern = Pattern.compile("\\[git\\-p4:\\s+depot\\-paths.+change\\s+=\\s+(\\d+)\\]");
        int counter = 0;
        String lastCommitText = commitText(counter++, false);
        Matcher matcher = gitP4Pattern.matcher(lastCommitText);
        while (!matcher.find()) {
            lastCommitText = commitText(counter++, false);
            if (StringUtils.isBlank(lastCommitText)) {
                throw new IllegalArgumentException("Failed to find last submitted changelist");
            }
            matcher.reset(lastCommitText);
        }
        String changelistId = matcher.group(1);
        String ref = MatcherUtils.singleMatchExpected(lastCommitText, "commit\\s+(\\w+)");
        return new String[] {ref, changelistId};
    }

    public String lastCommitText(boolean prettyPrint) {
        return commitText(0, prettyPrint);
    }

    public String commitText(int skipCount, boolean prettyPrint) {
        String prettyPrintCommand = prettyPrint ? " --pretty=%B\n" : "";
        return executeScmCommand("git log -1 --skip={}{}", String.valueOf(skipCount), prettyPrintCommand);
    }

    public String currentBranch() {
        return executeScmCommand("git rev-parse --abbrev-ref HEAD");
    }

    public String configValue(String propertyName) {
        if (!isGitInstalled()) {
            log.debug("Returning empty string for git config value {} as git is not installed", propertyName);
            return "";
        }
        return executeScmCommand("git config " + propertyName);
    }

    public String addConfigValue(String propertyName, String propertyValue) {
        return executeScmCommand("git config {} {}", propertyName, propertyValue);
    }

    public int totalCommitCount() {
        String commitCount = executeScmCommand("git rev-list HEAD --count");
        return Integer.parseInt(commitCount);
    }

    public Map<String, String> configValues() {
        if (!isGitInstalled()) {
            log.debug("Returning empty maps for git config values as git is not installed");
            return new HashMap<>();
        }

        String configText = executeScmCommand("git config -l");
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

    public void addChangesToDefaultChangelist(String origin) {
        String output = executeScmCommand("git p4 submit -M --prepare-p4-only --origin={}", origin);
        if (!output.contains("P4 workspace prepared for submission")) {
            log.error("Failed to apply commit to perforce, expected text \"P4 workspace prepared for submission\" in output\n{}", output);
            System.exit(1);
        }
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

    public byte[] diffAsByteArray(String parentRef, String commitRef, boolean supportsRenames) {
        String output = diff(parentRef, commitRef, supportsRenames);
        return output != null ? output.getBytes(Charset.forName("UTF8")) : null;
    }

    public String diff(String parentRef, String commitRef, boolean supportsRenames) {
        String renamesFlag = supportsRenames ? "-M " : "--no-renames ";
        String diffCommand = "git diff %s--no-color --full-index --no-ext-diff --ignore-submodules %s..%s";
        diffCommand = String.format(diffCommand, renamesFlag, parentRef, commitRef);
        String diffOutput = executeScmCommand(diffCommand);
        return diffOutput.length() > 0 ? diffOutput : null;
    }

    public String diff(String commitRef, boolean supportsRenames) {
        String renamesFlag = supportsRenames ? "-M " : "--no-renames ";
        String diffCommand = "git diff " + renamesFlag + "--no-color --full-index --no-ext-diff --ignore-submodules " + commitRef;
        return executeScmCommand(diffCommand);
    }

    public void submit(String origin) {
        String output = executeScmCommand("git p4 submit -M --conflict=quit --origin=" + origin, LogLevel.INFO);
        if (!output.contains("All commits applied!")) {
            log.error("git p4 submit failed!");
            System.exit(1);
        }
        log.info("Successfully ran git p4 submit");
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

    public void forcePushToRemoteBranch(String remoteBranch) {
        pushToRemoteBranch(remoteBranch, true);
    }

    public String mergeBase(String upstreamBranch, String commitRef) {
        return executeScmCommand("git merge-base " + upstreamBranch + " " + commitRef);
    }

    public String revParse(String commitRef) {
        return executeScmCommand("git rev-parse " + commitRef);
    }

    public String fetch() {
        return executeScmCommand("git fetch");
    }

    public String rebase(String branch) {
        return executeScmCommand("git rebase " + branch);
    }

    public String p4Rebase() {
        return executeScmCommand("git p4 rebase");
    }

    /**
     * Updates tags used by git changeset.
     */
    public void updateGitChangesetTagsMatchingRevision(String revision, LogLevel logLevel) {
        for (String tag : listTags()) {
            if (!tag.startsWith("changeset-")) {
                continue;
            }
            String tagRevision = revParse(tag);
            if (tagRevision.equals(revision)) {
                updateTag(tag, logLevel);
            }
        }
    }

    public String updateTag(String tagName, LogLevel logLevel) {
        if (StringUtils.isBlank(tagName)) {
            log.debug("Ignoring empty tag");
            return "no tag name specified";
        }
        if (tagName.equals("null")) {
            throw new RuntimeException("tag name should not equal null");
        }
        return executeScmCommand("git tag -f " + tagName, logLevel);
    }

    public List<String> listTags() {
        String output = executeScmCommand("git tag");
        return Arrays.asList(output.split("\n"));
    }

    public String deleteTag(String tagName) {
        return executeScmCommand("git tag -d " + tagName);
    }

    public String changesetCommand(String command, LogLevel logLevel) {
        return executeScmCommand("git changeset " + command, logLevel);
    }

    public String getTrackingBranch() {
        String branchName = currentBranch();
        String headRef = revParse("HEAD");

        String branchOutput = executeScmCommand("git branch -vv");

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
        return executeScmCommand("git init");
    }

    public String addAllFiles() {
        return executeScmCommand("git add --all");
    }

    public String reset(String ref) {
        return executeScmCommand("git reset --hard " + ref);
    }

    public List<FileChange> getStagedChanges() {
        return getChanges(false);
    }

    public List<FileChange> getAllChanges() {
        return getChanges(true);
    }

    public List<FileChange> getChangesInDiff(String fromRef, String toRef) {
        String output = executeScmCommand("git diff-tree --full-index -r -M -C {} {}", fromRef, toRef);
        Matcher lineMatcher = Pattern.compile(":(\\d+)\\s+(\\d+)\\s+\\w+\\s\\w+\\s+(\\w+)\\s+(.+)").matcher(output);
        List<FileChange> fileChanges = new ArrayList<>();
        while (lineMatcher.find()) {
            String oldFileMode = lineMatcher.group(1);
            String fileMode = lineMatcher.group(2);
            String actionTypeText = lineMatcher.group(3);
            String affectedFiles = lineMatcher.group(4);
            if (actionTypeText.startsWith("R")) {
                int similarityPercent = Integer.parseInt(actionTypeText.substring(1));
                FileChangeType changeType = similarityPercent == 100 ? FileChangeType.renamed : FileChangeType.renamedAndModified;
                fileChanges.add(new FileChange(scmType, fileMode, changeType, affectedFiles.split("[\t\n]")));
            } else {
                FileChangeType changeType = FileChangeType.changeTypeFromGitValue(actionTypeText);
                if (changeType == FileChangeType.deleted) {
                    fileChanges.add(new FileChange(scmType, oldFileMode, changeType, affectedFiles));
                } else {
                    fileChanges.add(new FileChange(scmType, fileMode, changeType, affectedFiles));
                }
            }
        }
        return fileChanges;
    }

    @Override
    protected void exitIfCommandFailed(String gitOutput) {
        if (StringUtils.isBlank(gitOutput)) {
            return;
        }

        if (gitOutput.trim().startsWith("fatal: Not a git repository")) {
            log.error("{} is not in a git repository", System.getProperty("user.dir"));
            System.exit(1);
        }
    }

    private void pushToRemoteBranch(String remoteBranch, boolean forceUpdate) {
        String currentHeadRef = revParse("HEAD");
        log.info("Pushing commit {} to {}", currentHeadRef, remoteBranch);

        String forceUpdateString = forceUpdate ? " -f" : "";
        String pushCommand = String.format("git push origin head:%s%s --porcelain", remoteBranch, forceUpdateString);

        String pushOutput = executeScmCommand(pushCommand, LogLevel.INFO);

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

    private List<FileChange> getChanges(boolean includeUnStagedChanges) {
        List<FileChange> changes = new ArrayList<>();
        String gitStatusOutput = executeScmCommand("git status --porcelain" );

        String pattern = String.format("^(\\s*)(%s+)\\s+(.+)", FileChangeType.allValuesAsGitPattern());
        Matcher changesMatcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(gitStatusOutput);

        while (changesMatcher.find()) {
            String leadingWhitespace = changesMatcher.group(1);
            String changesText = changesMatcher.group(2);
            String filePath = changesMatcher.group(3);

            FileChangeType fileChangeType = FileChangeType.changeTypeFromGitValue(changesText);
            // leading whitespace means that the change is unStaged
            if (includeUnStagedChanges || leadingWhitespace.isEmpty()) {
                int arrowIndex = filePath.indexOf("->");
                FileChange fileChange;
                final String FILE_MODE_UNKNOWN = "";
                if (arrowIndex != -1) {
                    String renamedFromFile = filePath.substring(0, arrowIndex).trim();
                    String renamedToFile = filePath.substring(arrowIndex + 3).trim();
                    fileChange = new FileChange(scmType, FILE_MODE_UNKNOWN, fileChangeType, renamedFromFile, renamedToFile);
                } else {
                    fileChange = new FileChange(scmType, FILE_MODE_UNKNOWN, fileChangeType, filePath);
                }
                changes.add(fileChange);
            }
        }

        return Collections.unmodifiableList(changes);
    }

    private boolean isGitInstalled() {
        if (gitInstalled != null) {
            return gitInstalled;
        }
        gitInstalled = isCommandAvailable("git");

        return gitInstalled;
    }

    private void exitIfNotInRepoRootFolder(String reason) {
        String rootDirectoryPath = getRootDirectory().getPath();
        if (!workingDirectory.getPath().equals(rootDirectoryPath)) {
            log.error("Working directory {} does not match root directory {}", workingDirectory.getPath(), rootDirectoryPath);
            throw new RuntimeException(reason);
        }
    }

    private void executeCommitCommand(String commitCommand, String msg) {
        executeScmCommand(commitCommand + " --file=-", msg, LogLevel.DEBUG);
    }

    private class GitDirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory() && file.getName().equals(".git");
        }
    }

}

package com.vmware.util.scm;

import com.vmware.util.CommandLineUtils;
import com.vmware.util.FileUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.logging.LogLevel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private File rootDirectory;

    public Git() {
        super(ScmType.git);
        super.setWorkingDirectory(System.getProperty("user.dir"));
        determineRootDirectory();
    }

    public Git(File rootDirectory) {
        super(ScmType.git);
        super.setWorkingDirectory(rootDirectory);
        this.rootDirectory = rootDirectory;
    }

    public boolean workingDirectoryIsInGitRepo() {
        return isGitInstalled() && getRootDirectory() != null;
    }

    /**
     * @return The root directory for this repo. Null if this is not a repo
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    public String newTrackingBranch(String branchName, String trackingBranch) {
        return executeScmCommand("checkout -b {} {}", branchName, trackingBranch);
    }

    public String applyPartialPatchFile(File patchFile) {
        return executeScmCommand("apply --ignore-whitespace --reject " + patchFile.getPath(), LogLevel.DEBUG);
    }

    public String applyPatchFile(File patchFile, boolean check) {
        String checkString = check ? " --check" : "";
        return executeScmCommand("apply --ignore-whitespace {} " + patchFile.getPath(), LogLevel.DEBUG, checkString);
    }

    public String applyPatch(String patchData, boolean check) {
        String checkString = check ? " --check" : "";
        return executeScmCommand("apply --ignore-whitespace -3{}", patchData, LogLevel.DEBUG, checkString);
    }

    public String diffTree(String fromRef, String ref, boolean binaryPatch, LogLevel level) {
        checkRefsAreValid(fromRef, ref);
        String binaryFlag = binaryPatch ? " --binary" : "";
        return executeScmCommand("diff-tree -M{} --full-index -U3 -p {} {}",level, binaryFlag, fromRef, ref) + "\n";
    }

    public String hashObject(File file) {
        return executeScmCommand("hash-object {}", file.getPath());
    }

    public void catFile(String fromRef, String filePath, String toFilePath) {
        checkRefsAreValid(fromRef);
        String command = String.format("git cat-file -p %s:%s", fromRef, filePath);
        Process statusProcess = CommandLineUtils.executeCommand(workingDirectory, null, command, (String) null);
        try {
            File toFile = new File(toFilePath);
            if (!toFile.exists() && toFile.getParentFile() != null) {
                toFile.getParentFile().mkdirs();
            }
            Files.copy(statusProcess.getInputStream(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public String applyDiffToPerforce(String rootDirectory, String diffData, boolean check) {
        exitIfNotInRepoRootFolder("apply must be run from the root folder " + getRootDirectory().getPath());
        String checkString = check ? " --check" : "";
        String output = executeScmCommand("apply --ignore-whitespace --directory={}{}", diffData, LogLevel.DEBUG, rootDirectory, checkString);
        if (StringUtils.isEmpty(output)) {
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

    public GitChangelistRef lastSubmittedChangelistInfo() {
        Pattern gitP4Pattern = Pattern.compile("\\[git\\-p4:\\s+depot\\-paths.+change\\s+=\\s+(\\d+)\\]");
        Pattern fusionPattern = Pattern.compile("\\s*Change:\\s*(\\d+)");
        int counter = 0;
        String lastCommitText = commitText(counter++);
        Matcher gitP4Matcher = gitP4Pattern.matcher(lastCommitText);
        Matcher fusionMatcher = fusionPattern.matcher(lastCommitText);
        String changelistId = null;
        while (changelistId == null) {
            if (gitP4Matcher.find()) {
                changelistId = gitP4Matcher.group(1);
            } else if (fusionMatcher.find()) {
                changelistId = fusionMatcher.group(1);
            } else {
                lastCommitText = commitText(counter++);
                if (StringUtils.isEmpty(lastCommitText)) {
                    throw new FatalException("Failed to find last submitted changelist");
                }
                gitP4Matcher.reset(lastCommitText);
                fusionMatcher.reset(lastCommitText);
            }
        }
        String ref = MatcherUtils.singleMatchExpected(lastCommitText, "commit\\s+(\\w+)");
        return new GitChangelistRef(ref, changelistId);
    }

    public String lastCommitText() {
        return commitText(0);
    }

    public String lastCommitBody() {
        return commitTextBody(0);
    }

    public String commitText(int skipCount) {
        return executeScmCommand("log -1 --skip={} --pretty=\"commit %H%nAuthor: %an <%ae>%nDate: %ad%n%B\" --shortstat --date=local",
                String.valueOf(skipCount)).trim();
    }

    public List<String> commitsSince(Date date) {
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZ").format(date);
        String commitsOutput = executeScmCommand("log --pretty=\";break;commit %H%nAuthor: %an <%ae>%nDate: %ad%n%B\" --shortstat --date=local --since={}",
                formattedDate);
        return Arrays.asList(commitsOutput.split(";break;"));
    }

    public String commitTextBody(int skipCount) {
        return executeScmCommand("log -1 --skip={} --pretty=%B", String.valueOf(skipCount)).trim();
    }

    public String currentBranch() {
        return executeScmCommand("rev-parse --abbrev-ref HEAD");
    }

    public String configValue(String propertyName) {
        if (!isGitInstalled()) {
            log.debug("Returning empty string for git config value {} as git is not installed", propertyName);
            return "";
        }
        return executeScmCommand("config " + propertyName);
    }

    public String addConfigValue(String propertyName, String propertyValue) {
        return executeScmCommand("config {} {}", propertyName, propertyValue);
    }

    public int totalCommitCount() {
        String commitCount = executeScmCommand("rev-list HEAD --count");
        return Integer.parseInt(commitCount);
    }

    public Map<String, String> configValues() {
        if (!isGitInstalled()) {
            log.debug("Returning empty maps for git config values as git is not installed");
            return new HashMap<>();
        }

        String configText = executeScmCommand("config -l");
        String[] valuesAsText = configText.split("[\r\n]+");
        Map<String, String> values = new HashMap<String, String>();
        for (String valueAsText : valuesAsText) {
            String[] valuePieces = valueAsText.split("=");
            if (valuePieces.length == 2) {
                values.put(valuePieces[0], valuePieces[1]);
            } else if (valuePieces.length == 1) {
                values.put(valuePieces[0], "");
            } else {
                log.debug("{} git config value could not be parsed", valueAsText);
            }
        }
        return values;
    }

    public void commit(String msg) {
        executeCommitCommand("commit", msg);
    }

    public void addChangesToDefaultChangelist(String origin) {
        String output = executeScmCommand("p4 submit -M --prepare-p4-only --origin={}", origin);
        if (!output.contains("P4 workspace prepared for submission")) {
            log.error("Failed to apply commit to perforce, expected text \"P4 workspace prepared for submission\" in output\n{}", output);
            System.exit(1);
        }
    }

    public void commitWithAllFileChanges(String msg) {
        executeCommitCommand("commit --all", msg);
    }

    public void amendCommit(String msg) {
        executeCommitCommand("commit --amend", msg);
    }

    public void amendCommitWithAllFileChanges(String msg) {
        executeCommitCommand("commit --amend --all", msg);
    }

    public byte[] diffAsByteArray(String parentRef, String commitRef, boolean supportsRenames) {
        String output = diff(parentRef, commitRef, supportsRenames);
        if (StringUtils.isNotBlank(output)) {
            output += "\n";
        }
        return output != null ? output.getBytes(Charset.forName("UTF8")) : null;
    }

    public String diff(String parentRef, String commitRef, boolean supportsRenames) {
        checkRefsAreValid(parentRef, commitRef);
        String renamesFlag = supportsRenames ? "-M " : "--no-renames ";
        String diffCommand = "diff %s--no-color --full-index --no-ext-diff --ignore-submodules %s..%s";
        diffCommand = String.format(diffCommand, renamesFlag, parentRef, commitRef);
        String diffOutput = executeScmCommand(diffCommand, LogLevel.TRACE);
        return diffOutput.length() > 0 ? diffOutput : null;
    }

    public String diff(String commitRef, boolean supportsRenames) {
        checkRefsAreValid(commitRef);
        String renamesFlag = supportsRenames ? "-M " : "--no-renames ";
        String diffCommand = "diff " + renamesFlag + "--no-color --full-index --no-ext-diff --ignore-submodules " + commitRef;
        return executeScmCommand(diffCommand);
    }

    public void submit(String origin) {
        String output = executeScmCommand("p4 submit -M --conflict=quit --origin=" + origin, LogLevel.INFO);
        if (!output.contains("All commits applied!")) {
            log.error("git p4 submit failed!");
            System.exit(1);
        }
        log.info("Successfully ran git p4 submit");
    }

    public void deleteRemoteBranch(String remote, String remoteBranch) {
        String pushCommand = String.format("push %s :%s --porcelain", remote, remoteBranch);
        executeScmCommand(pushCommand, LogLevel.INFO);
    }

    public void pushToRemoteBranch(String remote, String remoteBranch) {
        pushToRemoteBranch(remote, remoteBranch, false);
    }

    public void forcePushToRemoteBranch(String remote, String remoteBranch) {
        pushToRemoteBranch(remote, remoteBranch, true);
    }

    public String mergeBase(String upstreamBranch, String commitRef) {
        checkRefsAreValid(upstreamBranch, commitRef);
        return executeScmCommand("merge-base " + upstreamBranch + " " + commitRef);
    }

    public String revParseWithoutException(String commitRef) {
        return executeScmCommand("rev-parse " + commitRef);
    }

    public String revParse(String commitRef) {
        String output =  revParseWithoutException(commitRef);
        if (output.contains("ambiguous argument")) {
            throw new FatalException("Commit ref {} is not a valid ref: {}", commitRef, output);
        }
        return output;
    }

    public String fetch() {
        return executeScmCommand("fetch");
    }

    public String rebase(String branch) {
        return executeScmCommand("rebase " + branch);
    }

    public String p4Rebase() {
        return executeScmCommand("p4 rebase");
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
        if (StringUtils.isEmpty(tagName)) {
            log.debug("Ignoring empty tag");
            return "no tag name specified";
        }
        if (tagName.equals("null")) {
            throw new RuntimeException("tag name should not equal null");
        }
        return executeScmCommand("tag -f " + tagName, logLevel);
    }

    public List<String> listTags() {
        String output = executeScmCommand("tag");
        return Arrays.asList(output.split("\n"));
    }

    public String deleteTag(String tagName) {
        return executeScmCommand("tag -d " + tagName);
    }

    public String changesetCommand(String command, LogLevel logLevel) {
        return executeScmCommand("changeset " + command, logLevel);
    }

    public String getTrackingBranch() {
        String branchName = currentBranch();
        String headRef = revParse("HEAD");

        String branchOutput = executeScmCommand("branch -vv");

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
        return executeScmCommand("init");
    }

    public String addAllFiles() {
        return executeScmCommand("add --all");
    }

    public String reset(String ref) {
        return executeScmCommand("reset --hard " + ref);
    }

    public List<FileChange> getStagedChanges() {
        return getChanges(false);
    }

    public List<FileChange> getAllChanges() {
        return getChanges(true);
    }

    public List<FileChange> getChangesInDiff(String fromRef, String toRef) {
        String output = executeScmCommand("diff-tree --full-index -r -M -C {} {}", fromRef, toRef);
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
                } else if (changeType == FileChangeType.copied) {
                    fileChanges.add(new FileChange(scmType, oldFileMode, changeType, affectedFiles.split("[\t\n]")));
                } else {
                    fileChanges.add(new FileChange(scmType, fileMode, changeType, affectedFiles));
                }
            }
        }
        return fileChanges;
    }

    @Override
    public String fullPath(String pathWithinScm) {
        return getRootDirectory() + File.separator + pathWithinScm;
    }

    @Override
    protected String checkIfCommandFailed(String gitOutput) {
        if (StringUtils.isEmpty(gitOutput)) {
            return null;
        }

        if (gitOutput.trim().startsWith("fatal:")) {
            return System.getProperty("user.dir") + " is not in a git repository";
        }
        return null;
    }

    @Override
    protected String scmExecutablePath() {
        if (rootDirectory != null && workingDirectory.getPath().contains(rootDirectory.getPath())) {
            return "git";
        } else {
            return "git -C " + workingDirectory.getPath();
        }
    }

    private void pushToRemoteBranch(String remote, String remoteBranch, boolean forceUpdate) {
        String currentHeadRef = revParse("HEAD");
        log.info("Pushing commit {} to {}", currentHeadRef, remoteBranch);

        String forceUpdateString = forceUpdate ? " -f" : "";
        String pushCommand = String.format("push %s head:%s%s --porcelain", remote, remoteBranch, forceUpdateString);

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
            throw new FatalException("Git push failed, remote branch ref of {} does not match local head ref",
                    updatedRemoteHeadRef, currentHeadRef);
        }
        log.info("Remote branch was successfully updated");
    }

    private List<FileChange> getChanges(boolean includeUnStagedChanges) {
        List<FileChange> changes = new ArrayList<>();
        String gitStatusOutput = executeScmCommand("status --porcelain" );

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
                } else if (fileChangeType == FileChangeType.copied) {
                    String[] affectedFiles = filePath.split("\t");
                    fileChange = new FileChange(scmType, FILE_MODE_UNKNOWN, fileChangeType, affectedFiles[0], affectedFiles[1]);
                } else {
                    fileChange = new FileChange(scmType, FILE_MODE_UNKNOWN, fileChangeType, filePath);
                }
                changes.add(fileChange);
            }
        }

        return Collections.unmodifiableList(changes);
    }

    public Map<String, String> getLastCommitInfo() {
        Map<String, String> commitInfo = new LinkedHashMap<>();
        String commitText = lastCommitText();
        String summary = lastCommitBody();
        if (summary.contains("\n")) {
            summary = summary.substring(0, summary.indexOf('\n'));
        }
        commitInfo.put("Commit Date", MatcherUtils.singleMatchExpected(commitText, "Date:\\s+(.+)"));
        commitInfo.put("Summary", summary);
        commitInfo.put("Author", MatcherUtils.singleMatchExpected(commitText, "Author:\\s+(.+)"));
        return commitInfo;
    }

    public boolean isGitInstalled() {
        if (gitInstalled != null) {
            return gitInstalled;
        }
        gitInstalled = isCommandAvailable("git");

        return gitInstalled;
    }

    private void checkRefsAreValid(String... refs) {
        for (String ref : refs) {
            revParse(ref);
        }
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

    private void determineRootDirectory() {
        String rootDirectoryPath = CommandLineUtils.executeCommand("git rev-parse --show-toplevel", LogLevel.DEBUG);
        String commandCheckOutput = checkIfCommandFailed(rootDirectoryPath);
        rootDirectory = commandCheckOutput == null ? new File(rootDirectoryPath) : null;
    }

}

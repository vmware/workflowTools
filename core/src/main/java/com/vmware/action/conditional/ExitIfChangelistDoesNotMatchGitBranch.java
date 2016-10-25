package com.vmware.action.conditional;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.scm.FileChange;
import com.vmware.scm.FileChangeType;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.scm.FileChangeType.added;
import static com.vmware.scm.FileChangeType.addedAndModified;
import static com.vmware.scm.FileChangeType.deleted;

@ActionDescription("Creates a diff for the changelist and compares it to a diff of the current git branch.")
public class ExitIfChangelistDoesNotMatchGitBranch extends BaseLinkedPerforceCommitAction {

    public ExitIfChangelistDoesNotMatchGitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Checking if changelist {} matches current git branch", draft.perforceChangelistId);
        String[] lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String fromRef = config.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo[0] : config.trackingBranchPath();
        if (config.syncChangelistToLatestInBranch) {
            log.info("Creating git diff against last submitted changelist {}", lastSubmittedChangelistInfo[1]);
        } else {
            log.info("Creating git diff against tracking branch {}", config.trackingBranchPath());
        }

        List<FileChange> fileChanges = perforce.getFileChangesForPendingChangelist(draft.perforceChangelistId);
        String rawGitDiff = git.diffTree(fromRef, "head", true, LogLevel.TRACE);
        String gitDiff;
        if (containsChangesOfType(fileChanges, FileChangeType.renamed)) {
            gitDiff = stripLinesStartingWith(rawGitDiff, "similarity index");
        } else {
            gitDiff = rawGitDiff;
        }
        log.info("Creating perforce diff for changelist {} in git format", draft.perforceChangelistId);
        String perforceDiff = perforce.diffChangelistInGitFormat(fileChanges, draft.perforceChangelistId, true, LogLevel.TRACE);
        if (!containsChangesOfType(fileChanges, FileChangeType.modified)) {
            perforceDiff = stripLinesStartingWith(perforceDiff, "File(s) not opened for edit");
        }
        if (StringUtils.equals(gitDiff, perforceDiff)) {
            log.info("Perforce diff matches git diff exactly");
            return;
        } else {
            log.info("Perforce diff didn't match git diff exactly, comparing diffs in terms of content");
        }

        String reasonForMotMatching = compareDiffContent(gitDiff, perforceDiff);
        if (reasonForMotMatching != null) {
            log.error("Perforce diff didn't match git diff\n{}", reasonForMotMatching);
            System.exit(0);
        } else if (containsChangesOfType(fileChanges, added, addedAndModified, deleted)) {
            log.info("Perforce diff matches git diff in terms of content, adding or deleting file causes diff ordering to be different for perforce.");
        } else {
            log.info("Perforce diff matches git diff in terms of content");
        }
    }

    private String compareDiffContent(String gitDiff, String perforceDiff) {
        Map<String, String> gitDiffAsMap = convertDiffToMap(gitDiff);
        Map<String, String> perforceDiffAsMap = convertDiffToMap(perforceDiff);
        return compareDiff(gitDiffAsMap, perforceDiffAsMap);
    }

    private String stripLinesStartingWith(String text, String... textsToCheckFor) {
        String paddedText = "\n" + text + "\n"; // pad with new lines so that searches work for start and end lines
        for (String textToCheckFor : textsToCheckFor) {
            paddedText = paddedText.replaceAll("\n" + Pattern.quote(textToCheckFor) + ".+\n", "\n");
        }
        return paddedText.substring(1, paddedText.length() - 1);
    }

    private Map<String, String> convertDiffToMap(String diff) {
        Matcher diffLineMatcher = Pattern.compile("diff --git\\s+.+b/(.+)").matcher("");
        String fileName = null;
        String fileDiff = null;
        Map<String, String> fileDiffs = new HashMap<>();
        for (String diffLine : diff.split("\n")) {
            diffLineMatcher.reset(diffLine);
            if (diffLineMatcher.find()) {
                if (fileName != null) {
                    fileDiffs.put(fileName, fileDiff);
                }
                fileName = diffLineMatcher.group(1);
                fileDiff = diffLine;
            } else if (fileDiff != null) {
                fileDiff += "\n" + diffLine;
            } else {
                fileDiff = diffLine;
            }
        }
        if (fileName != null) {
            fileDiffs.put(fileName, fileDiff);
        }
        return fileDiffs;
    }

    private String compareDiff(Map<String, String> gitDiff, Map<String, String> perforceDiff) {
        if (!gitDiff.keySet().equals(perforceDiff.keySet())) {
            Set<String> fileChangesNotInPerforce = new HashSet<>(gitDiff.keySet());
            fileChangesNotInPerforce.removeAll(perforceDiff.keySet());
            Set<String> fileChangesNotInGit = new HashSet<>(perforceDiff.keySet());
            fileChangesNotInGit.removeAll(gitDiff.keySet());
            String errorText = "File list for diff is different";
            if (!fileChangesNotInPerforce.isEmpty()) {
                errorText += "\nFiles present in git but missing in perforce " + fileChangesNotInPerforce.toString();
            }
            if (!fileChangesNotInGit.isEmpty()) {
                errorText += "\nFiles present in perforce but missing in git " + fileChangesNotInGit.toString();
            }
            return errorText;
        }
        for (String gitDiffFile : gitDiff.keySet()) {
            String gitDiffFileText = gitDiff.get(gitDiffFile);
            String perforceDiffFileText = perforceDiff.get(gitDiffFile);
            String reasonForNotMatching = compareDiffText(gitDiffFileText, perforceDiffFileText);
            if (reasonForNotMatching != null) {
                return gitDiffFile + " did not match\n" + reasonForNotMatching;
            }
        }
        return null;
    }

    private boolean containsChangesOfType(List<FileChange> changes, FileChangeType... changeTypes) {
        for (FileChange change : changes) {
            for (FileChangeType changeType : changeTypes) {
                if (change.getChangeType() == changeType) {
                    return true;
                }
            }
        }
        return false;
    }

    private String compareDiffText(String gitDiff, String perforceDiff) {
        if (StringUtils.equals(gitDiff, perforceDiff)) {
            return null;
        }

        String[] gitDiffLines = gitDiff.split("\n");
        String[] perforceDiffLines = perforceDiff.split("\n");
        String output = "";
        if (gitDiffLines.length != perforceDiffLines.length
                && !gitDiff.contains("+++ " + FileChange.NON_EXISTENT_FILE_IN_GIT)) {
            output = "lines count mismatch, perforce: " + perforceDiffLines.length + " git: " + gitDiffLines.length + "\n";
        }
        Iterator<String> gitDiffIterator = Arrays.asList(gitDiffLines).iterator();
        Iterator<String> perforceDiffIterator = Arrays.asList(perforceDiffLines).iterator();

        int lineCount = 0;
        String gitDiffLineAfterMoving = null;
        while (perforceDiffIterator.hasNext()) {
            String gitDiffLine;
            if (gitDiffLineAfterMoving != null) {
                gitDiffLine = gitDiffLineAfterMoving;
                gitDiffLineAfterMoving = null;
            } else if (gitDiffIterator.hasNext()) {
                gitDiffLine = gitDiffIterator.next();
            } else {
                return output + "same until extra lines in git diff";
            }

            lineCount++;
            String perforceDiffLine = perforceDiffIterator.next();

            if (!StringUtils.equals(perforceDiffLine, gitDiffLine)) {
                return lineCount + "\n" + perforceDiffLine + "\n" + gitDiffLine;
            }
            if (gitDiffLine.startsWith("+++ " + FileChange.NON_EXISTENT_FILE_IN_GIT)) {
                log.info("Ignoring delete file lines in git diff as the perforce diff will not have those");
                gitDiffLineAfterMoving = moveIteratorUntilLineStartsWith(gitDiffIterator, "diff --git");
            }
        }
        return "failed to find expected difference between git and perforce diff";
    }

    private String moveIteratorUntilLineStartsWith(Iterator<String> iterator, String valueToMatch) {
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.startsWith(valueToMatch)) {
                return line;
            }
        }
        return null;
    }
}

package com.vmware.action.conditional;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.scm.FileChange;
import com.vmware.scm.FileChangeType;
import com.vmware.scm.Git;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ActionDescription("Creates a diff for the changelist and compares it to a diff of the current git branch.")
public class ExitIfChangelistDoesNotMatchGitBranch extends BaseLinkedPerforceCommitAction {

    public ExitIfChangelistDoesNotMatchGitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String[] lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String fromRef = config.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo[0] : config.trackingBranch;
        if (config.syncChangelistToLatestInBranch) {
            log.info("Creating git diff against last submitted changelist {}", lastSubmittedChangelistInfo[1]);
        } else {
            log.info("Creating git diff against tracking branch {}", config.trackingBranch);
        }

        List<FileChange> fileChanges = perforce.getFileChangesForPendingChangelist(draft.perforceChangelistId);
        String rawGitDiff = git.diffTree(fromRef, "head", true);
        String gitDiff = rawGitDiff;
        if (containsChangeOfType(fileChanges, FileChangeType.renamed)) {
            gitDiff = stripLinesStartingWith(rawGitDiff.split("\n"), "similarity index");
        }
        log.info("Creating perforce diff for changelist {} in git format", draft.perforceChangelistId);
        String perforceDiff = perforce.diffChangelistInGitFormat(fileChanges, draft.perforceChangelistId, true);
        if (!containsChangeOfType(fileChanges, FileChangeType.modified)) {
            perforceDiff = stripLinesStartingWith(perforceDiff.split("\n"), "File(s) not opened for edit");
        }
        log.info("Checking if perforce diff matches git diff");
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
        } else {
            log.info("Perforce diff matches git diff in terms of content, might be file ordering or whitespace differences");
        }
    }

    private String compareDiffContent(String gitDiff, String perforceDiff) {
        Map<String, String> gitDiffAsMap = convertDiffToMap(gitDiff);
        Map<String, String> perforceDiffAsMap = convertDiffToMap(perforceDiff);
        return compareDiff(gitDiffAsMap, perforceDiffAsMap);
    }

    private String stripLinesStartingWith(String[] lines, String... textsToCheckFor) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            boolean foundLineToStrip = false;
            for (String textToCheckFor : textsToCheckFor) {
                if (line.startsWith(textToCheckFor)) {
                    foundLineToStrip = true;
                    break;
                }
            }
            if (!foundLineToStrip) {
                builder.append(line).append("\n");
            }
        }
        return builder.toString();
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
            return "File list for diff is different, perforce: " + perforceDiff.keySet().toString()
                    + " git: " + gitDiff.keySet().toString();
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

    private boolean containsChangeOfType(List<FileChange> changes, FileChangeType changeType) {
        for (FileChange change : changes) {
            if (change.getChangeType() == changeType) {
                return true;
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

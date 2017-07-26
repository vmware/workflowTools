package com.vmware.action.conditional;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.scm.FileChange;
import com.vmware.scm.FileChangeType;
import com.vmware.scm.GitChangelistRef;
import com.vmware.scm.diff.PendingChangelistToGitDiffCreator;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.scm.FileChange.containsChangesOfType;
import static com.vmware.scm.FileChangeType.added;
import static com.vmware.scm.FileChangeType.addedAndModified;
import static com.vmware.scm.FileChangeType.deleted;
import static com.vmware.util.StringUtils.stripLinesStartingWith;

@ActionDescription("Creates a diff for the changelist and compares it to a diff of the current git branch.")
public class ExitIfChangelistDoesNotMatchGitBranch extends BaseLinkedPerforceCommitUsingGitAction {

    public ExitIfChangelistDoesNotMatchGitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Checking if changelist {} matches current git branch", draft.perforceChangelistId);
        GitChangelistRef lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String fromRef = config.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo.getCommitRef() : config.trackingBranchPath();
        if (config.syncChangelistToLatestInBranch) {
            log.info("Creating git diff against last submitted changelist {}", lastSubmittedChangelistInfo.getChangelistId());
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

        PendingChangelistToGitDiffCreator diffCreator = new PendingChangelistToGitDiffCreator(perforce);
        String perforceDiff = diffCreator.create(draft.perforceChangelistId, fileChanges, LogLevel.TRACE);
        if (StringUtils.equals(gitDiff, perforceDiff)) {
            log.info("Perforce diff matches git diff exactly");
            return;
        } else {
            log.info("Perforce diff didn't match git diff exactly, comparing diffs in terms of content");
        }

        String reasonForMotMatching = compareDiffContent(gitDiff, perforceDiff);
        if (reasonForMotMatching != null) {
            log.error("Perforce diff didn't match git diff\n{}\n", reasonForMotMatching);
            log.error("You might need to pull and rebase your git branch against the latest code.");
            System.exit(0);
        } else if (containsChangesOfType(fileChanges, added, addedAndModified, deleted)) {
            log.info("Perforce diff matches git diff in terms of content, adding or deleting file causes diff ordering to be different for perforce.");
        } else {
            log.info("Perforce diff matches git diff in terms of content.");
        }
    }

    private String compareDiffContent(String gitDiff, String perforceDiff) {
        Map<String, String> gitDiffAsMap = convertDiffToMap(gitDiff);
        Map<String, String> perforceDiffAsMap = convertDiffToMap(perforceDiff);
        return compareDiff(gitDiffAsMap, perforceDiffAsMap);
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
                return gitDiffFile + " did not match\n\n" + reasonForNotMatching;
            }
        }
        return null;
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
        List<String> perforceLines = new ArrayList<>();
        List<String> gitLines = new ArrayList<>();
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
            addDiffLine(perforceLines, perforceDiffLine);
            addDiffLine(gitLines, gitDiffLine);
            if (!StringUtils.equals(perforceDiffLine, gitDiffLine)) {
                addFollowingLines(perforceLines, perforceDiffIterator);
                addFollowingLines(gitLines, gitDiffIterator);
                String lineDifference = "DIFF DIFFERENCE \n(perforce line " + lineCount + ")\n"
                        + perforceDiffLine + "\n(git line " + lineCount + ")\n" + gitDiffLine;
                String perforceDiffText = "\n**** PERFORCE DIFF SAMPLE ****\n" + StringUtils.join(perforceLines, "\n");
                String gitDiffText = "\n**** GIT DIFF SAMPLE ****\n" + StringUtils.join(gitLines, "\n");
                return lineDifference + "\n" + perforceDiffText + "\n" + gitDiffText;
            }
            if (gitDiffLine.startsWith("+++ " + FileChange.NON_EXISTENT_FILE_IN_GIT)) {
                log.info("Ignoring delete file lines in git diff as the perforce diff will not have those");
                gitDiffLineAfterMoving = moveIteratorUntilLineStartsWith(gitDiffIterator, "diff --git");
            }
        }
        return "failed to find expected difference between git and perforce diff";
    }

    private void addFollowingLines(List<String> lines, Iterator<String> diffIterator) {
        int count = 0;
        while (count++ < 3 && diffIterator.hasNext()) {
            lines.add(diffIterator.next());
        }
    }

    private void addDiffLine(List<String> lines, String line) {
        if (lines.size() >= 3) {
            lines.remove(0);
        }
        lines.add(line);
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

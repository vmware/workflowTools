package com.vmware.util.scm.diff;

import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.FileChangeType;
import com.vmware.util.scm.Perforce;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.FileUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.util.scm.FileChange.containsChangesOfType;
import static com.vmware.util.scm.FileChangeType.added;
import static com.vmware.util.scm.FileChangeType.addedAndModified;
import static com.vmware.util.scm.FileChangeType.deleted;
import static com.vmware.util.StringUtils.stripLinesStartingWith;
import static java.lang.String.format;

/**
 * Creates a git diff for an open changelist.
 */
public class PendingChangelistToGitDiffCreator {

    private static final String NON_EXISTENT_INDEX_IN_GIT = "0000000000000000000000000000000000000000";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private Perforce perforce;

    public PendingChangelistToGitDiffCreator(Perforce perforce) {
        this.perforce = perforce;
    }

    public String create(String changelistId, LogLevel level) {
        List<FileChange> fileChanges = perforce.getFileChangesForPendingChangelist(changelistId);
        return create(changelistId, fileChanges, level);
    }

    public String create(String changelistId, List<FileChange> fileChanges, LogLevel level) {
        if (fileChanges.isEmpty()) {
            log.warn("No open file changes for changelist {}", changelistId);
            return "";
        }
        String filesToDiff = convertToText(fileChanges);
        String diffData = perforce.diffFilesUsingGit(filesToDiff, true, level);
        StringBuilder gitCompatibleData = null;
        String[] diffLines = diffData.split("\n");
        Iterator<String> diffLinesIterator = Arrays.asList(diffLines).iterator();
        while (diffLinesIterator.hasNext()) {
            String diffLine = diffLinesIterator.next();
            if (diffLine.startsWith("---") || diffLine.startsWith("+++ b/")
                    || diffLine.startsWith("index ") || diffLine.startsWith("diff --git")) {
                continue; // strip minus lines, plus lines, existing a and b lines, handled by perforce plus line
            }
            if (gitCompatibleData != null) {
                gitCompatibleData.append("\n");
            } else {
                gitCompatibleData = new StringBuilder();
            }
            if (!diffLine.startsWith("+++ ")) {
                gitCompatibleData.append(diffLine);
                continue;
            }
            boolean foundDiffHeader = false;
            for (FileChange fileChange : fileChanges) {
                if (!diffLine.startsWith("+++ " + perforce.fullPath(fileChange.getLastFileAffected()))) {
                    continue;
                }
                foundDiffHeader = true;
                createFullGitHeaderInfo(diffLinesIterator, fileChange, gitCompatibleData);
                break;
            }
            if (!foundDiffHeader) {
                throw new RuntimeException("Expected to convert plus line " + diffLine + " to git diff header format");
            }
        }

        appendDiffForAddedOrDeletedFiles(fileChanges, true, gitCompatibleData);
        if (gitCompatibleData == null) {
            return "";
        }
        String diffText = gitCompatibleData.append("\n").toString();
        if (!containsChangesOfType(fileChanges, FileChangeType.modified)) {
            diffText = stripLinesStartingWith(diffText, "File(s) not opened for edit");
        }
        return diffText;
    }

    private String convertToText(List<FileChange> fileChanges) {
        String filesToDiff = "";
        for (FileChange fileChange : fileChanges) {
            if (!filesToDiff.isEmpty()) {
                filesToDiff += " ";
            }
            filesToDiff += fileChange.getLastFileAffected();
        }
        return filesToDiff;
    }

    private void createFullGitHeaderInfo(Iterator<String> diffLinesIterator, FileChange fileChange,
                                         StringBuilder gitCompatibleData) {
        gitCompatibleData.append(fileChange.diffGitLine());
        String nextLine = diffLinesIterator.hasNext() ? diffLinesIterator.next() : null;
        if (nextLine != null && nextLine.startsWith("diff --git")) {
            nextLine = diffLinesIterator.hasNext() ? diffLinesIterator.next() : null;
        }
        if (nextLine != null && nextLine.startsWith("index ")) {
            gitCompatibleData.append("\n").append(nextLine);
            nextLine = diffLinesIterator.hasNext() ? diffLinesIterator.next() : null;
        }
        if (nextLine != null && nextLine.startsWith("--- ")) {
            gitCompatibleData.append("\n").append(fileChange.createMinusPlusLines());
        }
    }

    /**
     * Perforce's diff doesn't include a diff for added or deleted files. Mind boggling.
     */
    private void appendDiffForAddedOrDeletedFiles(List<FileChange> fileChanges, boolean binaryPatch,
                                                  StringBuilder gitCompatibleData) {
        Queue<String> fileDiffs = new ConcurrentLinkedQueue<>();
        fileChanges.parallelStream().forEach(fileChange -> {
            String fileDiff = determineFileDiff(binaryPatch, fileChange);
            if (fileDiff != null) {
                fileDiffs.add(fileDiff);
            }
        });

        fileDiffs.forEach(fileDiff -> gitCompatibleData.append(fileDiff));
    }

    private String determineFileDiff(boolean binaryPatch, FileChange fileChange) {
        FileChangeType changeType = fileChange.getChangeType();
        if (changeType != deleted && changeType != added && changeType != addedAndModified) {
            return null;
        }

        File tempEmptyFile = FileUtils.createTempFile("empty", ".file");
        String tempEmptyFilePath = tempEmptyFile.getPath();

        String firstFile, secondFile;
        File tempFileForDeleteComparison = null;
        if (changeType == deleted) {
            tempFileForDeleteComparison = FileUtils.createTempFile("deleteCompare", ".file");
            perforce.printToFile(perforce.fullPath(fileChange.getFirstFileAffected()), tempFileForDeleteComparison);
            firstFile = tempFileForDeleteComparison.getPath();
            secondFile = tempEmptyFilePath;
        } else {
            firstFile = tempEmptyFilePath;
            secondFile = perforce.fullPath(fileChange.getLastFileAffected());
        }
        String fileDiff = gitDiff(firstFile, secondFile, binaryPatch);
        if (StringUtils.isEmpty(fileDiff) && changeType == added) {
            log.warn("No content in added file {}", fileChange.getLastFileAffected());
            return null;
        }
        if (tempFileForDeleteComparison != null && !tempFileForDeleteComparison.delete()) {
            log.warn("Failed to delete temporary file created to diff delete for file {}: {}",
                    fileChange.getFirstFileAffected(), tempFileForDeleteComparison.getPath());
        }
        String newFileMode = MatcherUtils.singleMatch(fileDiff, "new mode (\\d+)");
        String indexPattern = "index\\s+(\\w+)\\.\\.(\\w+)";
        if (newFileMode == null) {
            indexPattern += "\\s+(\\d+)";
        }
        Matcher indexLineMatcher = Pattern.compile(indexPattern).matcher(fileDiff);
        if (!indexLineMatcher.find()) {
            throw new RuntimeException("Failed to match index line in diff\n" + fileDiff);
        }

        String firstIndex = changeType == deleted ? indexLineMatcher.group(1) : NON_EXISTENT_INDEX_IN_GIT;
        String secondIndex = changeType == deleted ? NON_EXISTENT_INDEX_IN_GIT : indexLineMatcher.group(2);
        String fileMode = newFileMode != null ? newFileMode : indexLineMatcher.group(3);
        fileChange.setFileMode(fileMode);

        StringBuilder fullDiffBuilder = new StringBuilder();
        fullDiffBuilder.append("\n").append(fileChange.diffGitLine());
        fullDiffBuilder.append("\n").append(format("index %s..%s", firstIndex, secondIndex));

        if (!fileChange.isBinaryFileType()) {
            fullDiffBuilder.append("\n").append(fileChange.createMinusPlusLines());
        }
        int indexToStartDiffAt = determineDiffStart(fileDiff);
        if (!tempEmptyFile.delete()) {
            log.warn("Failed to delete temporary empty file created for diffing files, {}", tempEmptyFile.getPath());
        }
        return fullDiffBuilder.append("\n").append(fileDiff.substring(indexToStartDiffAt)).toString();
    }

    private int determineDiffStart(String fileDiff) {
        String[] validStartValues = new String[] {"@@", "Binary Files", "GIT binary patch"};
        for (String validStartValue : validStartValues) {
            int index = fileDiff.indexOf("\n" + validStartValue);
            if (index != -1) {
                return index + 1;
            }
        }
        throw new RuntimeException("Failed to determine start for diff");
    }

    private String gitDiff(String firstFile, String secondFile, boolean binaryPatch) {
        String binaryFlag = binaryPatch ? " --binary" : "";
        return CommandLineUtils.executeCommand(null, format("git diff --full-index%s %s %s", binaryFlag,
                firstFile, secondFile), null, LogLevel.TRACE);
    }

}

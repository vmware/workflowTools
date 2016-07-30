package com.vmware.scm.diff;

import com.vmware.scm.FileChange;
import com.vmware.scm.FileChangeType;
import com.vmware.scm.Perforce;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.FileUtils;
import com.vmware.util.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.scm.FileChangeType.added;
import static com.vmware.scm.FileChangeType.addedAndModified;
import static com.vmware.scm.FileChangeType.deleted;
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

    public String create(String diffData, List<FileChange> fileChanges, boolean binaryPatch) {
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

        appendDiffForAddedOrDeletedFiles(fileChanges, binaryPatch, gitCompatibleData);
        return gitCompatibleData != null ? gitCompatibleData.append("\n").toString() : "";
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
        File tempEmptyFile = FileUtils.createTempFile("empty", ".file");
        String tempEmptyFilePath = tempEmptyFile.getPath();

        for (FileChange fileChange : fileChanges) {
            FileChangeType changeType = fileChange.getChangeType();
            if (changeType != deleted && changeType != added && changeType != addedAndModified) {
                continue;
            }

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
            if (tempFileForDeleteComparison != null && !tempFileForDeleteComparison.delete()) {
                log.warn("Failed to delete temporary file created to diff delete for file {}: {}",
                        fileChange.getFirstFileAffected(), tempFileForDeleteComparison.getPath());
            }
            Matcher indexLineMatcher = Pattern.compile("index\\s+(\\w+)\\.\\.(\\w+)\\s+(\\d+)").matcher(fileDiff);
            if (!indexLineMatcher.find()) {
                throw new RuntimeException("Failed to match index line in diff");
            }

            String firstIndex = changeType == deleted ? indexLineMatcher.group(1) : NON_EXISTENT_INDEX_IN_GIT;
            String secondIndex = changeType == deleted ? NON_EXISTENT_INDEX_IN_GIT : indexLineMatcher.group(2);
            String fileMode = indexLineMatcher.group(3);
            fileChange.setFileMode(fileMode);
            gitCompatibleData.append("\n").append(fileChange.diffGitLine());
            gitCompatibleData.append("\n").append(format("index %s..%s", firstIndex, secondIndex));

            if (!fileChange.isBinaryFileType()) {
                gitCompatibleData.append("\n").append(fileChange.createMinusPlusLines());
            }
            int indexToStartDiffAt = determineDiffStart(fileDiff);
            gitCompatibleData.append("\n").append(fileDiff.substring(indexToStartDiffAt));
        }
        if (!tempEmptyFile.delete()) {
            log.warn("Failed to delete temporary empty file created for diffing files, {}", tempEmptyFile.getPath());
        }
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
                firstFile, secondFile), null, LogLevel.DEBUG);
    }

}

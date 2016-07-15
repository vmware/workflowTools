package com.vmware.scm;

import com.vmware.util.CommandLineUtils;
import com.vmware.util.FileUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.scm.FileChangeType.added;
import static com.vmware.scm.FileChangeType.addedAndModified;
import static com.vmware.scm.FileChangeType.deleted;
import static com.vmware.scm.FileChangeType.deletedAfterRename;
import static com.vmware.scm.FileChangeType.renamed;
import static java.lang.String.format;

/**
 * Wrapper around p4 commands
 */
public class Perforce extends BaseScmWrapper {

    private String clientName;

    public Perforce(String clientName) {
        super(ScmType.perforce);
        this.clientName = clientName;
        if (StringUtils.isBlank(clientName)) {
            throw new IllegalArgumentException("perforceClientName config value is not set, can also be set by git-p4.client git config value");
        }
        File workingDirectory = getClientDirectory();
        if (workingDirectory == null) {
            throw new IllegalArgumentException("Failed to find root directory for perforce client " + clientName);
        }
        super.setWorkingDirectory(workingDirectory);
    }

    public List<String> getPendingChangelists() {
        String changeLists = executeScmCommand("p4 changes -c {} -s pending", clientName);
        if (StringUtils.isBlank(changeLists)) {
            return Collections.emptyList();
        }
        List<String> changeListIds = new ArrayList<>();
        for (String line : changeLists.split("\n")) {
            changeListIds.add(MatcherUtils.singleMatch(line, "Change\\s+(\\d+)\\s+on"));
        }
        return changeListIds;
    }

    public String readLastPendingChangelist() {
        String output = executeScmCommand("p4 changes -m 1 -s pending -l -c {}", clientName);
        output = output.replaceAll("\n\t", "\n");
        return output;
    }

    public String readChangelist(String changelistId) {
        String output = executeScmCommand("p4 describe -s {}", changelistId);
        output = output.replaceAll("\n\t", "\n");
        return output;
    }

    public String printToFile(String fileToPrint, File outputFile) {
        return executeScmCommand("p4 print -o {} {}", outputFile.getPath(), fileToPrint);
    }

    public void deletePendingChangelist(String changelistId) {
        executeScmCommand("p4 change -d " + changelistId, LogLevel.INFO);
    }

    public void revertChangesInPendingChangelist(String changelistId) {
        executeScmCommand("p4 revert -w -c {} //...", LogLevel.INFO, changelistId);
    }

    public void revertFile(String changelistId, String file) {
        executeScmCommand("p4 revert -w -c {} {}", changelistId, file);
    }

    public String createPendingChangelist(String description, boolean filesExpected) {
        String perforceTemplate = executeScmCommand("p4 change -o");
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, filesExpected);
        String output = executeScmCommand("p4 change -i", amendedTemplate, LogLevel.DEBUG);
        return changeSucceeded(output) ? MatcherUtils.singleMatch(output, "Change\\s+(\\d+)\\s+created") : null;
    }

    public void clean() {
        executeScmCommand("p4 clean //...", LogLevel.INFO);
    }

    public void moveAllOpenFilesToChangelist(String changelistId) {
        executeScmCommand("p4 reopen -c " + changelistId + " //...", LogLevel.INFO);
    }

    public String getFileInfo(String filePath) {
        return executeScmCommand("p4 files " + filePath);
    }

    public String getWhereFileInfo(String filePath) {
        return executeScmCommand("p4 where " + filePath);
    }

    public String getCurrentChangelistId(String id) {
        String changelistText = executeScmCommand("p4 change -o -O " + id);
        return MatcherUtils.singleMatch(changelistText, "Change:\\s+(\\d+)");
    }

    public String getChangelistStatus(String id) {
        String changelistText = executeScmCommand("p4 change -o -O " + id);
        return MatcherUtils.singleMatch(changelistText, "Status:\\s+(pending|submitted)");
    }

    public List<FileChange> getFileChangesForPendingChangelist(String id) {
        String filesText = executeScmCommand("p4 -ztag opened -c {}", id);
        Matcher lineMatcher = Pattern.compile("\\.\\.\\.\\s+(\\w+)\\s+(.+)").matcher(filesText);
        FileChange fileChange = null;
        List<FileChange> fileChanges = new ArrayList<>();
        String clientNameToStrip = "//" + clientName + File.separator;
        while (lineMatcher.find()) {
            String valueName = lineMatcher.group(1);
            String value = lineMatcher.group(2);
            if (valueName.equals("depotFile")) {
                if (fileChange != null) {
                    fileChanges.add(fileChange);
                }
                fileChange = new FileChange(scmType);
            }
            if (fileChange != null) {
                fileChange.parseValue(valueName, value, clientNameToStrip);
            }
        }
        if (fileChange != null) {
            fileChanges.add(fileChange);
        }
        mergeMoveDeleteAndAdds(fileChanges);

        return fileChanges;
    }

    public String diffChangelistInGitFormat(String changelistId, boolean binaryPatch) {
        List<FileChange> fileChanges = getFileChangesForPendingChangelist(changelistId);
        return diffChangelistInGitFormat(fileChanges, changelistId, binaryPatch);
    }

    public String diffChangelistInGitFormat(List<FileChange> fileChanges, String changelistId, boolean binaryPatch) {
        String filesToDiff = "";
        if (fileChanges.isEmpty()) {
            log.warn("No open file changes for changelist {}", changelistId);
            return "";
        }
        for (FileChange fileChange : fileChanges) {
            if (!filesToDiff.isEmpty()) {
                filesToDiff += " ";
            }
            filesToDiff += fullPath(fileChange.getLastFileAffected());
        }
        String diffData = diffFilesUsingGit(filesToDiff, binaryPatch);
        return convertDiffToGitFormat(diffData, fileChanges, binaryPatch);
    }

    private String diffFilesUsingGit(String filesToDiff, boolean binaryPatch) {
        Map<String, String> environmentVariables = new HashMap<>();
        String binaryFlag = binaryPatch ? " --binary" : "";
        environmentVariables.put("P4DIFF", "git diff --full-index" + binaryFlag);
        return executeScmCommand(environmentVariables, "p4 diff -du", filesToDiff, LogLevel.DEBUG);
    }

    private void mergeMoveDeleteAndAdds(List<FileChange> fileChanges) {
        for (FileChange fileChange : fileChanges) {
            if (fileChange.getChangeType() != renamed) {
                continue;
            }
            String moveDepotFile = fileChange.getFirstFileAffected();
            boolean foundMatchingDeleteFile = false;
            for (FileChange matchingDeleteChange : fileChanges) {
                if (matchingDeleteChange.getChangeType() == deletedAfterRename
                        && moveDepotFile.equals(matchingDeleteChange.getDepotFile())) {
                    foundMatchingDeleteFile = true;
                    String deleteClientFile = matchingDeleteChange.getLastFileAffected();
                    fileChange.replaceFileAffected(0, deleteClientFile);
                    break;
                }
            }
            if (!foundMatchingDeleteFile) {
                throw new RuntimeException("Expected to find matching move/delete action for move depot file " + moveDepotFile);
            }
        }

        Iterator<FileChange> changeIterator = fileChanges.iterator();
        while (changeIterator.hasNext()) {
            FileChange fileChange = changeIterator.next();
            if (fileChange.getChangeType() == FileChangeType.deletedAfterRename) {
                changeIterator.remove();
            }
        }
    }

    public String sync(String fileNames) {
        return executeScmCommand("p4 sync {}", fileNames);
    }

    public String move(String changelistId, String fromFileName, String toFileName, String extraFlags) {
        String output = executeScmCommand("p4 move {} -c {} {} {}", extraFlags, changelistId, fromFileName, toFileName);
        return failOutputIfMissingText(output, "moved from");
    }

    public String add(String changelistId, String fileName) {
        String output = executeScmCommand("p4 add -c {} {}", changelistId, fileName);
        return failOutputIfMissingText(output, "opened for add");
    }

    public String openForEdit(String changelistId, String fileName) {
        String output = executeScmCommand("p4 edit -c {} {}", changelistId, fileName);
        return failOutputIfMissingText(output, "opened for edit");
    }

    public String markForDelete(String changelistId, String fileName) {
        String output = executeScmCommand("p4 delete -c {} {}", changelistId, fileName);
        return failOutputIfMissingText(output, "opened for delete");
    }

    public boolean updatePendingChangelist(String id, String description) {
        String perforceTemplate = executeScmCommand("p4 change -o " + id);
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, false);
        String output = executeScmCommand("p4 change -i", amendedTemplate, LogLevel.DEBUG);
        return changeSucceeded(output);
    }

    public void submitChangelist(String id, String description) {
        String perforceTemplate = executeScmCommand("p4 change -o " + id);
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, true);
        String submitOutput = executeScmCommand("p4 submit -f revertunchanged -i", amendedTemplate, LogLevel.INFO);
        String status = getChangelistStatus(id);
        if (!"submitted".equals(status)) {
            log.error("Changelist {} has status {}, expected submitted", id, status);
            log.error("Submit output\n{}", submitOutput);
            System.exit(1);
        }
    }

    private boolean changeSucceeded(String output) {
        if (output.contains("Error in change specification")) {
            log.error("Failed to apply change\n{}\n", output);
            return false;
        } else {
            return true;
        }
    }

    private File getClientDirectory() {
        String info = executeScmCommand("p4 clients -e " + clientName, LogLevel.DEBUG);
        String clientDirectory = MatcherUtils.singleMatch(info, "Client\\s+" + clientName + "\\s+.+?(\\S+)\\s+'Created by");
        if (clientDirectory != null) {
            return new File(clientDirectory);
        } else {
            log.warn("{} not found in clients list\n{}", clientName, info);
            return null;
        }
    }

    private String convertDiffToGitFormat(String diffData, List<FileChange> fileChanges, boolean binaryPatch) {
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
                if (!diffLine.startsWith("+++ " + fullPath(fileChange.getLastFileAffected()))) {
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
                printToFile(fullPath(fileChange.getFirstFileAffected()), tempFileForDeleteComparison);
                firstFile = tempFileForDeleteComparison.getPath();
                secondFile = tempEmptyFilePath;
            } else {
                firstFile = tempEmptyFilePath;
                secondFile = fullPath(fileChange.getLastFileAffected());
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

    private String updateTemplateWithDescription(String perforceTemplate, String commitText, boolean filesExpected) {
        int descriptionIndex = perforceTemplate.indexOf("Description:");
        if (descriptionIndex == -1) {
            throw new IllegalArgumentException("Failed to find Description: in perforce template:\n" + perforceTemplate);
        }
        int filesIndex = perforceTemplate.indexOf("Files:");
        if (filesIndex == -1 && filesExpected) {
            throw new IllegalArgumentException("Failed to find Files: in perforce template, does the git commit have file changes?:\n" + perforceTemplate);
        } else if (filesIndex == -1) {
            log.debug("No files detected");
            filesIndex = perforceTemplate.length();
        }

        String amendedCommitText = "\t" + commitText.replaceAll("\n", "\n\t");

        return format("%sDescription:\n%s\n%s",
                perforceTemplate.substring(0, descriptionIndex), amendedCommitText, perforceTemplate.substring(filesIndex));
    }

    private String gitDiff(String firstFile, String secondFile, boolean binaryPatch) {
        String binaryFlag = binaryPatch ? " --binary" : "";
        return CommandLineUtils.executeCommand(null, format("git diff --full-index%s %s %s", binaryFlag,
                firstFile, secondFile), null, LogLevel.DEBUG);
    }
}

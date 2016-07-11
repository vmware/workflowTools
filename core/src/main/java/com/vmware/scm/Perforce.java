package com.vmware.scm;

import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.scm.FileChangeType.deletedAfterRename;
import static com.vmware.scm.FileChangeType.renamed;

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

    public List<FileChange> getFileChangesForChangelist(String id) {
        String filesText = executeScmCommand("p4 -ztag opened -c {}", id);
        Matcher lineMatcher = Pattern.compile("\\.\\.\\.\\s+(\\w+)\\s+(.+)").matcher(filesText);
        FileChange fileChange = null;
        List<FileChange> fileChanges = new ArrayList<>();
        String clientNameToStrip = "//" + clientName + File.separator;
        while (lineMatcher.find()) {
            String valueName = lineMatcher.group(1);
            String value = lineMatcher.group(2);
            switch (valueName) {
                case "depotFile":
                    if (fileChange != null) {
                        fileChanges.add(fileChange);
                    }
                    fileChange = new FileChange(scmType);
                    fileChange.addFileAffected(0, value);
                    break;
                case "clientFile":
                    assert fileChange != null;
                    String strippedPath = value.substring(clientNameToStrip.length());
                    fileChange.addFileAffected(1, strippedPath);
                    break;
                case "action":
                    assert fileChange != null;
                    fileChange.setChangeType(FileChangeType.changeTypeFromPerforceValue(value));
                    break;
                case "movedFile":
                    assert fileChange != null;
                    fileChange.addFileAffected(1, value);
                    break;
                case "rev":
                    assert fileChange != null;
                    fileChange.setFileVersion(Integer.parseInt(value));
                    break;
            }
        }
        if (fileChange != null) {
            fileChanges.add(fileChange);
        }
        mergeMoveDeleteAndAdds(fileChanges);

        return fileChanges;
    }

    private void mergeMoveDeleteAndAdds(List<FileChange> fileChanges) {
        for (FileChange fileChange : fileChanges) {
            if (fileChange.getChangeType() != renamed) {
                continue;
            }
            String moveDepotFile = fileChange.getFileAffected(1);
            boolean foundMatchingDeleteFile = false;
            for (FileChange matchingDeleteChange : fileChanges) {
                if (matchingDeleteChange.getChangeType() == deletedAfterRename
                        && matchingDeleteChange.getFileAffected(0).equals(moveDepotFile)) {
                    foundMatchingDeleteFile = true;
                    String deleteClientFile = matchingDeleteChange.getFileAffected(2);
                    fileChange.replaceFileAffected(1, deleteClientFile);
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
            fileChange.removeFileAffected(0);
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

        return String.format("%sDescription:\n%s\n%s",
                perforceTemplate.substring(0, descriptionIndex), amendedCommitText, perforceTemplate.substring(filesIndex));
    }
}

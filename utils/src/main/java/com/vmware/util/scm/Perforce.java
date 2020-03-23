package com.vmware.util.scm;

import com.vmware.util.scm.diff.PendingChangelistToGitDiffCreator;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
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

import static com.vmware.util.scm.FileChangeType.deletedAfterRename;
import static com.vmware.util.scm.FileChangeType.renamed;
import static com.vmware.util.StringUtils.appendWithDelimiter;
import static com.vmware.util.logging.LogLevel.DEBUG;
import static com.vmware.util.logging.LogLevel.INFO;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Wrapper around p4 commands
 */
public class Perforce extends BaseScmWrapper {

    private static final Pattern whereDepotFileInfoPattern = Pattern.compile("(//.+?)\\s+");

    private String username;

    private String clientName;

    private boolean loggedIn;

    public Perforce(String clientDirectory) {
        this(null, clientDirectory);
    }

    public Perforce(String clientName, String clientDirectory) {
        super(ScmType.perforce);
        if (!CommandLineUtils.isCommandAvailable("p4")) {
            super.setWorkingDirectory(System.getProperty("user.dir"));
            return;
        }
        this.username = checkIfLoggedIn();
        this.loggedIn = username != null;
        if (!loggedIn) {
            super.setWorkingDirectory(System.getProperty("user.dir"));
            return;
        }
        if (StringUtils.isNotEmpty(clientName) && StringUtils.isNotEmpty(clientDirectory)) {
            this.clientName = clientName;
            super.setWorkingDirectory(clientDirectory);
        } else if (StringUtils.isNotEmpty(clientName)) {
            this.clientName = clientName;
            super.setWorkingDirectory(determineClientDirectoryForClientName());
        } else if (StringUtils.isNotEmpty(clientDirectory)) {
            super.setWorkingDirectory(clientDirectory);
        } else {
            Map<String, String> infoValues = info();
            this.clientName = infoValues.get("Client name");
            String workingDirectory = infoValues.containsKey("Client root") ? infoValues.get("Client root") : System.getProperty("user.dir");
            super.setWorkingDirectory(workingDirectory);
        }
    }

    public List<String> getPendingChangelists() {
        return getPendingChangelists(false);
    }

    private List<String> getPendingChangelists(boolean includeSummary) {
        String changeListText = executeScmCommand("changes -c {} -L -s pending", getClientName());
        if (StringUtils.isEmpty(changeListText)) {
            return Collections.emptyList();
        }
        Iterator<String> changelistsIterator = asList(changeListText.split("\n")).iterator();
        List<String> changeLists = new ArrayList<>();
        while (changelistsIterator.hasNext()) {
            String line = changelistsIterator.next();
            String changelist = MatcherUtils.singleMatch(line, "Change\\s+(\\d+)\\s+on");
            if (StringUtils.isEmpty(changelist)) {
                continue;
            }
            if (includeSummary) {
                String summary = null;
                while (changelistsIterator.hasNext() && StringUtils.isEmpty(summary)) {
                    summary = changelistsIterator.next();
                }
                if (summary != null) {
                    changelist += " " + summary.trim();
                }
            }
            changeLists.add(changelist);
        }
        return changeLists;
    }

    public String selectPendingChangelist() {
        List<String> changelistIds = getPendingChangelists(true);
        if (changelistIds.isEmpty()) {
            throw new RuntimeException("No pending change lists in client " + getClientName() + " to select from");
        }
        if (changelistIds.size() == 1) {
            return changelistIds.get(0);
        }
        int selectedIndex = InputUtils.readSelection(changelistIds, "Select changelist");
        return changelistIds.get(selectedIndex);
    }

    public String readChangelist(String changelistId) {
        String output = executeScmCommand("describe -s {}", changelistId);
        output = output.replaceAll("\n\t", "\n");
        return output.trim();
    }

    public Map<String, String> info() {
        String output = executeScmCommand("info");
        Map<String, String> values = new HashMap<>();
        for (String line : output.split("\n")) {
            String[] linePieces = line.split(":");
            if (linePieces.length != 2) {
                continue;
            }
            values.put(linePieces[0].trim(), linePieces[1].trim());
        }
        return values;
    }

    public String printToFile(String fileToPrint, File outputFile) {
        return executeScmCommand("print -o {} {}", outputFile.getPath(), fileToPrint);
    }

    public void deletePendingChangelist(String changelistId) {
        executeScmCommand("change -d " + changelistId, INFO);
    }

    public void revertChangesInPendingChangelist(String changelistId) {
        executeScmCommand("revert -w -c {} //...", INFO, changelistId);
    }

    public void revertFiles(String changelistId, List<String> filePaths) {
        executeScmCommand("revert -w -c {} {}", changelistId, appendWithDelimiter("", filePaths, " "));
    }

    public void revertFiles(List<String> filePaths) {
        executeScmCommand("revert -w {}", appendWithDelimiter("", filePaths, " "));
    }

    public String createPendingChangelist(String description, boolean filesExpected) {
        String perforceTemplate = executeScmCommand("change -o");
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, filesExpected);
        String output = executeScmCommand("change -i", amendedTemplate, DEBUG);
        return changeSucceeded(output) ? MatcherUtils.singleMatch(output, "Change\\s+(\\d+)\\s+created") : null;
    }

    public void clean() {
        executeScmCommand("clean //...", INFO);
    }

    public void reopenAllOpenFilesInChangelist(String changelistId) {
        executeScmCommand("reopen -c " + changelistId + " //...", INFO);
    }

    public String reopen(String changelistId, List<String> filePaths) {
        String output = executeScmCommand("reopen -c {} {}", changelistId, appendWithDelimiter("", filePaths, " "));
        return failOutputIfMissingText(output, asList("reopened; change " + changelistId, "nothing changed"), filePaths.size());
    }

    public String getFileInfo(String filePath) {
        return executeScmCommand("files " + filePath);
    }

    public Map<String, String> getWhereDepotFileInfoForRelativePaths(List<String> filePaths) {
        String filePathTexts = appendWithDelimiter("", filePaths, " ");
        String[] whereFileOutput = executeScmCommand("where " + filePathTexts).split("\n");
        return addMatchedValuesToMap(filePaths, asList(whereFileOutput), whereDepotFileInfoPattern);
    }

    public String fstat(List<String> fileNames) {
        return executeScmCommand("fstat {}", StringUtils.appendWithDelimiter("", fileNames, " "));
    }

    public List<String> getOpenedFilesInClient() {
        return parseFileNamesFromOpenedOutput(executeScmCommand("opened"));
    }

    public List<String> getOpenedFilesInChangelist(String changelistId) {
        return parseFileNamesFromOpenedOutput(executeScmCommand("opened -c {}", changelistId));
    }

    public Map<String, List<FileChange>> getAllFileChangesInClient() {
        List<String> fileNames = getOpenedFilesInClient();
        String filesText = fstat(fileNames);
        List<FileChange> allChanges = parseFileChanges(filesText);
        if (allChanges.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<FileChange>> changeMap = new HashMap<>();
        for (FileChange change : allChanges) {
            String changelistId = change.getPerforceChangelistId();
            if (!changeMap.containsKey(changelistId)) {
                changeMap.put(changelistId, new ArrayList<FileChange>());
            }
            changeMap.get(changelistId).add(change);
        }
        // make lists read only
        for (String changelistId : changeMap.keySet()) {
            List<FileChange> fileChanges = changeMap.get(changelistId);
            changeMap.put(changelistId, Collections.unmodifiableList(fileChanges));
        }
        return Collections.unmodifiableMap(changeMap);
    }

    public String getCurrentChangelistId(String id) {
        String changelistText = executeScmCommand("change -o -O " + id);
        return MatcherUtils.singleMatch(changelistText, "Change:\\s+(\\d+)");
    }

    public String getChangelistStatus(String id) {
        String changelistText = executeScmCommand("change -o -O " + id);
        return MatcherUtils.singleMatch(changelistText, "Status:\\s+(pending|submitted)");
    }

    public List<FileChange> getFileChangesForPendingChangelist(String id) {
        List<String> filesInChangelist = getOpenedFilesInChangelist(id);
        String filesText = fstat(filesInChangelist);
        return parseFileChanges(filesText);
    }

    public String diffChangelistInGitFormat(String changelistId, LogLevel level) {
        PendingChangelistToGitDiffCreator diffCreator = new PendingChangelistToGitDiffCreator(this);
        return diffCreator.create(changelistId, level);
    }

    public String getClientName() {
        if (clientName != null) {
            return clientName;
        }

        this.clientName = determineClientNameForDirectory(username);
        return clientName;
    }

    @Override
    String checkIfCommandFailed(String output) {
        if (output.contains("Your session has expired, please login again")) {
            return "You need to relogin to Perforce, error message: " + output;
        }
        return null;
    }

    @Override
    protected String scmExecutablePath() {
        if (getWorkingDirectory() == null) {
            return "p4";
        } else {
            String workingDirectoryPath = getWorkingDirectory().getPath();
            if (!getWorkingDirectory().exists()) {
                throw new RuntimeException("Perforce client directory " + workingDirectoryPath + " doe not exist");
            }
            return "p4 -d " + workingDirectoryPath;
        }
    }

    private List<String> parseFileNamesFromOpenedOutput(String output) {
        List<String> fileNames = new ArrayList<>();
        Matcher fileNameMatcher = Pattern.compile("(\\S+)#\\d+\\s+").matcher(output);
        while (fileNameMatcher.find()) {
            fileNames.add(fileNameMatcher.group(1));
        }
        return fileNames;
    }

    private List<FileChange> parseFileChanges(String filesText) {
        Matcher lineMatcher = Pattern.compile("\\.\\.\\.\\s+(\\w+)\\s*(\\S+$)?", Pattern.MULTILINE).matcher(filesText);
        FileChange fileChange = null;
        List<FileChange> fileChanges = new ArrayList<>();
        while (lineMatcher.find()) {
            String valueName = lineMatcher.group(1);
            String value = lineMatcher.groupCount() > 1 ? lineMatcher.group(2) : null;
            if (valueName.equals("depotFile")) {
                if (fileChange != null) {
                    fileChanges.add(fileChange);
                }
                fileChange = new FileChange(scmType);
            }
            if (fileChange != null) {
                fileChange.parseValue(valueName, value, getWorkingDirectory().getPath());
            }
        }
        if (fileChange != null) {
            fileChanges.add(fileChange);
        }
        mergeMoveDeleteAndAdds(fileChanges);
        return fileChanges;
    }

    public String diffFilesUsingGit(String filesToDiff, boolean binaryPatch, LogLevel level) {
        Map<String, String> environmentVariables = new HashMap<>();
        String binaryFlag = binaryPatch ? " --binary" : "";
        environmentVariables.put("P4DIFF", "git diff --full-index" + binaryFlag);
        return executeScmCommand(environmentVariables, "diff -du " + filesToDiff, null, level);
    }

    private void mergeMoveDeleteAndAdds(List<FileChange> fileChanges) {
        for (FileChange fileChange : fileChanges) {
            if (fileChange.getChangeType() != renamed) {
                continue;
            }
            String movedDepotFile = fileChange.getFirstFileAffected();
            boolean foundMatchingDeleteFile = false;
            for (FileChange matchingDeleteChange : fileChanges) {
                if (matchingDeleteChange.getChangeType() == deletedAfterRename
                        && movedDepotFile.equals(matchingDeleteChange.getDepotFile())) {
                    foundMatchingDeleteFile = true;
                    String deleteClientFile = matchingDeleteChange.getLastFileAffected();
                    fileChange.replaceFileAffected(0, deleteClientFile);
                    break;
                }
            }
            if (!foundMatchingDeleteFile) {
                throw new RuntimeException("Expected to find matching move/delete action for moved depot file " + movedDepotFile);
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

    public String sync(List<String> filesToSync, String syncChangelistId) {
        String syncVersion = StringUtils.isEmpty(syncChangelistId) ? "" : "@" + syncChangelistId;
        String fileNames = appendWithDelimiter("", filesToSync, syncVersion + " ") + syncVersion;
        return executeScmCommand("sync -f {}", fileNames);
    }

    public String move(String changelistId, String fromFileName, String toFileName, String extraFlags) {
        String output = executeScmCommand("move {} -c {} {} {}", extraFlags, changelistId, fromFileName, toFileName);
        return failOutputIfMissingText(output, Arrays.asList("moved from", "already opened for move/delete"), 1);
    }

    public String add(String changelistId, String fileName) {
        String output = executeScmCommand("add -c {} {}", changelistId, fileName);
        // need to use reopen
        String otherChangelistId = MatcherUtils.singleMatch(output, "change from change (\\d+)");
        if (otherChangelistId != null) {
            log.info("Reopening file {} from changelist {} as it has already been added", fileName, otherChangelistId);
            return reopen(changelistId, Collections.singletonList(fileName));
        } else {
            return failOutputIfMissingText(output, "opened for add");
        }
    }

    public String openForEdit(String changelistId, String fileName) {
        String output = executeScmCommand("edit -c {} {}", changelistId, fileName);
        return failOutputIfMissingText(output, "opened for edit");
    }

    public String markForDelete(String changelistId, String fileName) {
        String output = executeScmCommand("delete -c {} {}", changelistId, fileName);
        if (output.contains("can't delete (already opened for edit)")) {
            log.info("Revert file {} as it is already opened for edit", fileName);
            revertFiles(Collections.singletonList(fileName));
            return markForDelete(changelistId, fileName);
        }

        String otherChangelistId = MatcherUtils.singleMatch(output, "change from change (\\d+)");
        if (otherChangelistId != null) {
            log.info("Reopening file {} from changelist {} as it has already been deleted", fileName, otherChangelistId);
            return reopen(changelistId, Collections.singletonList(fileName));
        } else {
            return failOutputIfMissingText(output, "opened for delete");
        }
    }

    public void syncPerforceFiles(List<FileChange> fileChanges, String syncChangelistId) {
        List<String> filesToSync = new ArrayList<>();
        for (FileChange diffChange : fileChanges) {
            filesToSync.add(fullPath(diffChange.getFirstFileAffected()));
            if (!diffChange.getLastFileAffected().equals(diffChange.getFirstFileAffected())) {
                filesToSync.add(fullPath(diffChange.getLastFileAffected()));
            }
        }

        if (filesToSync.isEmpty()) {
            return;
        }
        log.debug("Syncing existing perforce files {}", filesToSync.toString());
        sync(filesToSync, syncChangelistId);
    }

    public List<FileChange> revertAndResyncUnresolvedFiles(List<FileChange> changelistChanges, String versionToSyncTo) {
        if (changelistChanges == null || changelistChanges.isEmpty()) {
            return Collections.emptyList();
        }
        List<FileChange> unresolvedFileChanges = new ArrayList<>();
        List<String> unresolvedFilesToRevertAndSync = new ArrayList<>();
        for (int i = changelistChanges.size() - 1; i >= 0; i--) {
            FileChange fileChange = changelistChanges.get(i);
            if (fileChange.isUnresolved()) {
                fileChange.setPerforceChangelistId(null);
                unresolvedFileChanges.add(fileChange);
                unresolvedFilesToRevertAndSync.add(fileChange.getLastFileAffected());
            }
        }
        if (unresolvedFilesToRevertAndSync.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("Reverting and resyncing unresolved files: {}", unresolvedFilesToRevertAndSync.toString());
        revertFiles(unresolvedFilesToRevertAndSync);
        sync(unresolvedFilesToRevertAndSync, versionToSyncTo);
        return unresolvedFileChanges;
    }

    public void openFilesForEditIfNeeded(String changelistId, List<FileChange> fileChanges) {
        List<String> filePathsToOpenForEdit = new ArrayList<>();
        List<String> filesToMoveToChangelist = new ArrayList<>();

        for (FileChange diffChange : fileChanges) {
            if (changelistId.equals(diffChange.getPerforceChangelistId())) {
                continue;
            }
            if (!FileChangeType.isEditChangeType(diffChange.getChangeType())) {
                continue;
            }

            String fullPath = fullPath(diffChange.getFirstFileAffected());
            if (StringUtils.isNotEmpty(diffChange.getPerforceChangelistId())) {
                log.info("Moving file {} from changelist {}", diffChange.getFirstFileAffected(),
                        diffChange.getPerforceChangelistId());
                filesToMoveToChangelist.add(fullPath);
            } else {
                log.info("Opening file {} for edit", diffChange.getFirstFileAffected());
                filePathsToOpenForEdit.add(fullPath);
            }
        }
        if (!filesToMoveToChangelist.isEmpty()) {
            reopen(changelistId, filesToMoveToChangelist);
        }
        if (!filePathsToOpenForEdit.isEmpty()) {
            openForEdit(changelistId, appendWithDelimiter("", filePathsToOpenForEdit, " "));
        }
    }

    public void renameAddOrDeleteFiles(String changelistId, List<FileChange> fileChanges) {
        for (FileChange diffChange : fileChanges) {
            FileChangeType changeType = diffChange.getChangeType();
            String fullPathForFirstFileAffected = fullPath(diffChange.getFirstFileAffected());
            String fullPathForLastFileAffected = fullPath(diffChange.getLastFileAffected());
            if (changeType == FileChangeType.renamed || changeType == FileChangeType.renamedAndModified) {
                log.info("Renaming file {} to {}", diffChange.getFirstFileAffected(), diffChange.getLastFileAffected());
                move(changelistId, fullPathForFirstFileAffected, fullPathForLastFileAffected, "-k");
            } else if (FileChangeType.isAddChangeType(changeType)) {
                log.info("Adding file {} to perforce", diffChange.getLastFileAffected());
                add(changelistId, fullPathForLastFileAffected);
            } else if (changeType == FileChangeType.deleted) {
                log.info("Deleting {}", diffChange.getLastFileAffected());
                markForDelete(changelistId, fullPathForLastFileAffected);
            }
        }
    }

    public boolean updatePendingChangelist(String id, String description) {
        String perforceTemplate = executeScmCommand("change -o " + id);
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, false);
        String output = executeScmCommand("change -i", amendedTemplate, DEBUG);
        return changeSucceeded(output);
    }

    public void submitChangelist(String id, String description) {
        String perforceTemplate = executeScmCommand("change -o " + id);
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, true);
        String submitOutput = executeScmCommand("submit -f revertunchanged -i", amendedTemplate, INFO);
        String status = getChangelistStatus(id);
        if (!"submitted".equals(status)) {
            log.error("Changelist {} has status {}, expected submitted", id, status);
            log.error("Submit output\n{}", submitOutput);
            System.exit(1);
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getUsername() {
        return username;
    }

    private boolean changeSucceeded(String output) {
        if (output.contains("Error in change specification")) {
            log.error("Failed to apply change\n{}\n", output);
            return false;
        } else {
            return true;
        }
    }

    private String checkIfLoggedIn() {
        Process statusProcess = CommandLineUtils.executeCommand(workingDirectory, null, "p4 login -s", (String) null);
        try {
            String output = IOUtils.read(statusProcess.getInputStream());
            int exitValue = statusProcess.waitFor();
            if (exitValue != 0) {
                return null;
            }
            return MatcherUtils.singleMatchExpected(output, "User\\s(\\w+)\\s+ticket");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String determineClientDirectoryForClientName() {
        String info = executeScmCommand("clients -e " + clientName, DEBUG);
        String clientDirectory = MatcherUtils.singleMatch(info, "Client\\s+" + clientName + "\\s+.+?(\\S+)\\s+'Created by");
        if (clientDirectory == null) {
            clientDirectory = System.getProperty("user.dir");
            log.warn("Failed to parse client directory for client {} Using directory {}\n{}", clientName, clientDirectory, info);

        }
        return clientDirectory;
    }

    private String determineClientNameForDirectory(String username) {
        String clientRoot = super.getWorkingDirectory().getPath();
        String quotedClientRoot = Pattern.quote(clientRoot);
        String info = executeScmCommand("clients -u " + username, DEBUG);
        String clientName = MatcherUtils.singleMatch(info, "Client\\s+(\\S+)\\s+.+?" + quotedClientRoot + "\\s+'Created by");
        if (clientName == null) {
            throw new NoPerforceClientForDirectoryException(clientRoot, username, info);
        }
        return clientName;
    }

    private String updateTemplateWithDescription(String perforceTemplate, String commitText, boolean filesExpected) {
        int descriptionIndex = perforceTemplate.indexOf("Description:");
        if (descriptionIndex == -1) {
            throw new FatalException("Failed to find Description: in perforce template:\n" + perforceTemplate);
        }
        int filesIndex = perforceTemplate.indexOf("Files:");
        if (filesIndex == -1 && filesExpected) {
            throw new FatalException("Failed to find Files: in perforce template, does the git commit have file changes?:\n" + perforceTemplate);
        } else if (filesIndex == -1) {
            log.debug("No files detected");
            filesIndex = perforceTemplate.length();
        }

        String amendedCommitText = "\t" + commitText.replaceAll("\n", "\n\t");

        return format("%sDescription:\n%s\n%s",
                perforceTemplate.substring(0, descriptionIndex), amendedCommitText, perforceTemplate.substring(filesIndex));
    }

    private Map<String, String> addMatchedValuesToMap(List<String> sourceValues, List<String> outputTexts, Pattern pattern) {
        Matcher matcher = pattern.matcher("");
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < sourceValues.size(); i ++) {
            matcher.reset(outputTexts.get(i));
            if (!matcher.find()) {
                throw new RuntimeException("Failed to match pattern "
                        + matcher.pattern().pattern() + " in text " + outputTexts.get(i));
            }
            values.put(sourceValues.get(i), matcher.group(1));
        }
        return values;
    }
}

package com.vmware;

import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Wrapper around p4 commands
 */
public class Perforce extends BaseScmWrapper {
    public Perforce(File workingDirectory) {
        super(workingDirectory);
    }

    public List<String> getPendingChangelists(String clientName) {
        String changeLists = executeScmCommand("p4 changes -c " + clientName + " -s pending");
        if (StringUtils.isBlank(changeLists)) {
            return Collections.emptyList();
        }
        List<String> changeListIds = new ArrayList<>();
        for (String line : changeLists.split("\n")) {
            changeListIds.add(MatcherUtils.singleMatch(line, "Change\\s+(\\d+)\\s+on"));
        }
        return changeListIds;
    }

    public File getClientDirectory() {
        String info = executeScmCommand("p4 info", Level.FINE);
        String clientDirectory = MatcherUtils.singleMatch(info, "Client root:\\s+(.+)");
        return clientDirectory != null ? new File(clientDirectory) : null;
    }

    public String readLastPendingChangelist(String username) {
        String output = executeScmCommand("p4 changes -m 1 -s pending -l -u " + username);
        output = output.replaceAll("\n\t", "\n");
        return output;
    }

    public String readChangelist(String changelistId) {
        String output = executeScmCommand("p4 describe -s " + changelistId);
        output = output.replaceAll("\n\t", "\n");
        return output;
    }

    public void deletePendingChangelist(String changelistId) {
        executeScmCommand("p4 change -d " + changelistId, Level.INFO);
    }

    public void revertChangesInPendingChangelist(String changelistId) {
        executeScmCommand("p4 revert -w -c " + changelistId + " //...", Level.INFO);
    }

    public String createPendingChangelist(String description, boolean filesExpected) {
        String perforceTemplate = executeScmCommand("p4 change -o");
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, filesExpected);
        String output = executeScmCommand("p4 change -i", amendedTemplate, Level.FINE);
        return changeSucceeded(output) ? MatcherUtils.singleMatch(output, "Change\\s+(\\d+)\\s+created") : null;
    }

    public void moveAllOpenFilesToChangelist(String changelistId) {
        executeScmCommand("p4 reopen -c " + changelistId + " //...", Level.INFO);
    }

    public boolean updatePendingChangelist(String id, String description) {
        String perforceTemplate = executeScmCommand("p4 change -o " + id);
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, false);
        String output = executeScmCommand("p4 change -i", amendedTemplate, Level.FINE);
        return changeSucceeded(output);
    }

    public void submitChangelist(String id, String description) {
        String perforceTemplate = executeScmCommand("p4 change -o " + id);
        String amendedTemplate = updateTemplateWithDescription(perforceTemplate, description, true);
        executeScmCommand("p4 submit -f revertunchanged -i", amendedTemplate, Level.INFO);
    }

    private boolean changeSucceeded(String output) {
        if (output.contains("Error in change specification")) {
            log.error("Failed to apply change\n{}\n", output);
            return false;
        } else {
            return true;
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

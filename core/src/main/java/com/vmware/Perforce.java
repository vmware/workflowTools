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
    public Perforce() {
        super(new File(System.getProperty("user.dir")));
    }

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

    public void deletePendingChangelist(String changelistId) {
        executeScmCommand("p4 change -d " + changelistId, Level.INFO);
    }

    public void revertChangesInPendingChangelist(String changelistId) {
        executeScmCommand("p4 revert -w -c " + changelistId + " //...", Level.INFO);
    }

    public String createPendingChangelist(String commitText) {
        String perforceTemplate = executeScmCommand("p4 change -o");
        int descriptionIndex = perforceTemplate.indexOf("Description:");
        if (descriptionIndex == -1) {
            throw new IllegalArgumentException("Failed to find Description: in perforce template:\n" + perforceTemplate);
        }
        int filesIndex = perforceTemplate.indexOf("Files:");
        if (filesIndex == -1) {
            throw new IllegalArgumentException("Failed to find Files: in perforce template, does the git commit have file changes?:\n" + perforceTemplate);
        }

        String amendedCommitText = "\t" + commitText.replaceAll("\n", "\n\t");

        String amendedTemplate = String.format("%sDescription:\n%s\n%s",
                perforceTemplate.substring(0, descriptionIndex), amendedCommitText, perforceTemplate.substring(filesIndex));


        String output = executeScmCommand("p4 change -i", amendedTemplate, Level.FINE);
        boolean success = !output.contains("Error in change specification.");
        if (!success) {
            log.error("Failed to create pending changelist\n{}\n", output);
            return null;
        } else {
            String changelistId = MatcherUtils.singleMatch(output, "Change\\s+(\\d+)\\s+created");
            return changelistId;
        }
    }
}

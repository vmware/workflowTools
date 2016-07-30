package com.vmware.scm.diff;

import com.vmware.scm.Perforce;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Converts a git diff to the perforce diff format.
 */
public class GitDiffToPerforceConverter {

    private static final String[] VALUES_TO_IGNORE = new String[]{"diff --git", "index ", "deleted file mode", "new file mode"};
    private static final Pattern depotFileInfoPattern = Pattern.compile("(.+)#(\\d+)");

    private List<String> depotFilesToCheck = new ArrayList<>();
    private List<String> whereFilesToCheck = new ArrayList<>();
    private Map<String, String> depotMappings = new HashMap<>();
    private Map<String, String> depotVersions = new HashMap<>();
    private String diffDate;
    private String lastDiffFile;

    private Perforce perforce;
    private String lastSubmittedChangelist;

    public GitDiffToPerforceConverter(Perforce perforce, String lastSubmittedChangelist) {
        this.perforce = perforce;
        this.lastSubmittedChangelist = lastSubmittedChangelist;
    }

    public byte[] convert(String gitDiff) {
        if (gitDiff == null) {
            return null;
        } else if (gitDiff.isEmpty()) {
            return new byte[0];
        }
        depotFilesToCheck.clear();
        whereFilesToCheck.clear();

        diffDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        List<String> diffLines = Arrays.asList(gitDiff.split("\n"));
        lastDiffFile = "";
        String output = "";
        Iterator<String> linesIterator = diffLines.iterator();
        while (linesIterator.hasNext()) {
            String lineToAdd = convertDiffLine(linesIterator);
            output = appendLineToOutput(output, lineToAdd);
        }

        addPerforceDepotInfoForFiles();
        output = addDepotInfoToOutput(output);

        return output.getBytes(Charset.forName("UTF-8"));
    }

    private String addDepotInfoToOutput(String output) {
        for (String depotFileToCheck : depotMappings.keySet()) {
            String depotMapping = depotMappings.get(depotFileToCheck);
            if (depotMapping == null) {
                throw new IllegalArgumentException("No depot mapping for file " + depotFileToCheck);
            }
            String version = depotVersions.get(depotMapping);
            output = output.replace("[!!" + depotFileToCheck + "#0!!]", depotMapping + "#" + version);
            output = output.replace("[!!" + depotFileToCheck + "!!]", depotMapping);
        }
        return output;
    }

    private String convertDiffLine(Iterator<String> linesIterator) {
        String diffLine = linesIterator.next();
        String lineToAdd = null;
        String similarityIndex = MatcherUtils.singleMatch(diffLine, "similarity index (\\d+)%");
        String minusDiffFile = MatcherUtils.singleMatch(diffLine, "---\\s+a/(.+)");
        String addDiffFile = MatcherUtils.singleMatch(diffLine, "\\+\\+\\+\\s+b/(.+)");
        if (similarityIndex != null) {
            lineToAdd = createFileRenameText(linesIterator, similarityIndex);
        } else if (diffLine.startsWith("--- /dev/null")) {
            String addFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "\\+\\+\\+\\s+b/(.+)");
            addWhereFileToCheckIfNeeded(addFile);
            lineToAdd = createPerforceMinusFile(addFile) + "\n" + createPerforceAddFile(addFile);
        } else if (diffLine.startsWith("+++ /dev/null")) {
            lineToAdd = createPerforceAddFile(lastDiffFile);
        } else if (minusDiffFile != null) {
            addDepotFileForCheckingIfNeeded(minusDiffFile);
            lastDiffFile = minusDiffFile;
            lineToAdd = createPerforceMinusFile(minusDiffFile);
        } else if (addDiffFile != null) {
            lineToAdd = createPerforceAddFile(addDiffFile);
        } else if (diffLine.equals(" ")) {
            // removing just to be consistent with perforce diffs
            lineToAdd = "";
        } else if (!StringUtils.textStartsWithValue(diffLine, VALUES_TO_IGNORE)) {
            lineToAdd = diffLine;
        }
        return lineToAdd;
    }

    private String createFileRenameText(Iterator<String> linesIterator, String similarityIndex) {
        int similarityValue = Integer.parseInt(similarityIndex);
        String renameFromFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "rename from (.+)");
        String renameToFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "rename to (.+)");
        addDepotFileForCheckingIfNeeded(renameFromFile);
        addWhereFileToCheckIfNeeded(renameToFile);
        if (similarityValue == 100) {
            return format("==== [!!%s#0!!] ==MV== [!!%s!!] ====\n", renameFromFile, renameToFile);
        } else {
            linesIterator.next();
            linesIterator.next();
            String renamedDiffFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "\\+\\+\\+\\s+b/(.+)");
            if (!renameToFile.equals(renamedDiffFile)) {
                throw new IllegalArgumentException(
                        format("Expected renamed to file [%s] name to match +++ b/ file name[%s]", renameToFile, renamedDiffFile));
            }
            return format("Moved from: [!!%s!!]\nMoved to: [!!%s!!]\n%s\n%s", renameFromFile, renameToFile,
                    createPerforceMinusFile(renameFromFile), createPerforceAddFile(renameToFile));
        }
    }

    private void addDepotFileForCheckingIfNeeded(String diffFile) {
        if (!depotMappings.containsKey(diffFile)) {
            depotFilesToCheck.add(diffFile);
        }
    }

    private void addWhereFileToCheckIfNeeded(String diffFile) {
        if (!depotMappings.containsKey(diffFile)) {
            whereFilesToCheck.add(diffFile);
        }
    }

    private String appendLineToOutput(String output, String lineToAdd) {
        if (lineToAdd == null) {
            return output;
        }
        if (!output.isEmpty()) {
            output += "\n";
        }
        output += lineToAdd;
        return output;
    }

    private void addPerforceDepotInfoForFiles() {
        if (!depotFilesToCheck.isEmpty()) {
            String filesListToCheck = "";
            for (String depotFileToCheck : depotFilesToCheck) {
                if (!filesListToCheck.isEmpty()) {
                    filesListToCheck += " ";
                }
                filesListToCheck += format("%s/%s@%s", perforce.getWorkingDirectory(), depotFileToCheck, lastSubmittedChangelist);
            }
            String depotFilesInfo = perforce.getFileInfo(filesListToCheck);
            parsePerforceFilesOutput(depotFilesInfo);
        }

        if (!whereFilesToCheck.isEmpty()) {
            Map<String, String> whereFilesInfo = perforce.getWhereDepotFileInfoForRelativePaths(whereFilesToCheck);
            for (String filePath : whereFilesInfo.keySet()) {
                String depotMapping = whereFilesInfo.get(filePath);
                depotMappings.put(filePath, depotMapping);
                depotVersions.put(depotMapping, "0");
            }
        }
    }

    private void parsePerforceFilesOutput(String perforceOutput) {
        int counter = 0;
        for (String depotFileInfo : perforceOutput.split("\n")) {
            String depotFileChecked = depotFilesToCheck.get(counter++);
            Matcher infoMatcher = parseLine(depotFileInfo, depotFileInfoPattern);
            String depotMapping = infoMatcher.group(1);
            depotMappings.put(depotFileChecked, depotMapping);
            depotVersions.put(depotMapping, infoMatcher.group(2));
        }
    }

    private Matcher parseLine(String line, Pattern patternToUse) {
        Matcher matcher = patternToUse.matcher(line);
        if (!matcher.find()) {
            throw new RuntimeException("Failed to parse line " + line + " with pattern " + patternToUse.pattern()
                    + ", perforce working directory " + perforce.getWorkingDirectory() + " is being used");
        }
        return matcher;
    }

    private String createPerforceMinusFile(String minusDiffFile) {
        return format("--- [!!%s!!]\t[!!%s#0!!]", minusDiffFile, minusDiffFile);
    }

    private String createPerforceAddFile(String addDiffFile) {
        return "+++ [!!" + addDiffFile + "!!]\t" + diffDate;
    }
}

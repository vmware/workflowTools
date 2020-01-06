package com.vmware.util.scm.diff;

import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.FileChangeType;
import com.vmware.util.scm.Git;
import com.vmware.util.scm.Perforce;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

import static com.vmware.util.scm.ScmType.git;
import static java.lang.String.format;

/**
 * Converts a git diff to the perforce diff format.
 */
public class GitDiffToPerforceConverter implements DiffConverter {

    private static final String[] VALUES_TO_IGNORE = new String[]{"diff --git", "index ", "deleted file mode", "new file mode"};
    private static final Pattern depotFileInfoPattern = Pattern.compile("(.+)#(\\d+)");

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private List<String> depotFilesToCheck = new ArrayList<>();
    private List<String> whereFilesToCheck = new ArrayList<>();
    private Map<String, String> depotMappings = new HashMap<>();
    private Map<String, String> depotVersions = new HashMap<>();
    private List<FileChange> fileChanges;
    private String diffDate;
    private String lastDiffFile;

    private Perforce perforce;
    private String lastSubmittedChangelist;
    private String output;

    public GitDiffToPerforceConverter(Perforce perforce, String lastSubmittedChangelist) {
        this.perforce = perforce;
        this.lastSubmittedChangelist = lastSubmittedChangelist;
    }

    public String convert(String gitDiff) {
        if (gitDiff == null) {
            return null;
        } else if (gitDiff.isEmpty()) {
            return "";
        }
        depotFilesToCheck.clear();
        whereFilesToCheck.clear();
        fileChanges = new ArrayList<>();

        diffDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        List<String> diffLines = Arrays.asList(gitDiff.split("\n"));
        lastDiffFile = "";
        output = "";
        Iterator<String> linesIterator = diffLines.iterator();
        while (linesIterator.hasNext()) {
            String lineToAdd = convertDiffLine(linesIterator);
            appendLineToOutput(lineToAdd);
        }

        addPerforceDepotInfoForFiles();
        addDepotInfoToOutput();
        return output;
    }

    @Override
    public byte[] convertAsBytes(String diffData) {
        String convertedData = convert(diffData);
        return convertedData != null ? convertedData.getBytes(Charset.forName("UTF-8")) : null;
    }

    public List<FileChange> getFileChanges() {
        return fileChanges;
    }

    private void addDepotInfoToOutput() {
        for (String depotFileToCheck : depotMappings.keySet()) {
            String depotMapping = depotMappings.get(depotFileToCheck);
            if (depotMapping == null) {
                throw new FatalException("No depot mapping for file " + depotFileToCheck);
            }
            String version = depotVersions.get(depotMapping);
            output = output.replace("[!!" + depotFileToCheck + "#0!!]", depotMapping + "#" + version);
            output = output.replace("[!!" + depotFileToCheck + "!!]", depotMapping);
        }
    }

    private String convertDiffLine(Iterator<String> linesIterator) {
        String diffLine = linesIterator.next();
        String lineToAdd = null;
        String similarityIndex = MatcherUtils.singleMatch(diffLine, "similarity index (\\d+)%");
        String minusDiffFile = MatcherUtils.singleMatch(diffLine, "---\\s+a/(.+)");
        String addDiffFile = MatcherUtils.singleMatch(diffLine, "\\+\\+\\+\\s+b/(.+)");
        FileChange fileChange = null;
        if (similarityIndex != null) {
            lineToAdd = createFileRenameText(linesIterator, similarityIndex);
        } else if (diffLine.startsWith("--- /dev/null")) {
            String addFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "\\+\\+\\+\\s+b/(.+)");
            fileChange = new FileChange(git, FileChangeType.added, addFile);
            addWhereFileToCheckIfNeeded(addFile);
            lineToAdd = createPerforceMinusFile(addFile) + "\n" + createPerforceAddFile(addFile);
        } else if (diffLine.startsWith("+++ /dev/null")) {
            fileChange = new FileChange(git, FileChangeType.deleted, lastDiffFile);
            lineToAdd = createPerforceAddFile(lastDiffFile);
        } else if (minusDiffFile != null) {
            addDepotFileForCheckingIfNeeded(minusDiffFile);
            lastDiffFile = minusDiffFile;
            lineToAdd = createPerforceMinusFile(minusDiffFile);
        } else if (addDiffFile != null) {
            fileChange = new FileChange(git, FileChangeType.modified, addDiffFile);
            lineToAdd = createPerforceAddFile(addDiffFile);
        } else if (diffLine.equals(" ")) {
            // removing just to be consistent with perforce diffs
            lineToAdd = "";
        } else if (!StringUtils.textStartsWithValue(diffLine, VALUES_TO_IGNORE)) {
            lineToAdd = diffLine;
        }
        if (fileChange != null) {
            fileChanges.add(fileChange);
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
            fileChanges.add(new FileChange(git, FileChangeType.renamed, renameFromFile, renameToFile));
            return format("==== [!!%s#0!!] ==MV== [!!%s!!] ====\n", renameFromFile, renameToFile);
        } else {
            linesIterator.next();
            linesIterator.next();
            String renamedDiffFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "\\+\\+\\+\\s+b/(.+)");
            if (!renameToFile.equals(renamedDiffFile)) {
                throw new FatalException(
                        "Expected renamed to file [{}] name to match +++ b/ file name[{}]", renameToFile, renamedDiffFile);
            }
            fileChanges.add(new FileChange(git, FileChangeType.renamedAndModified, renameFromFile, renameToFile));
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

    private void appendLineToOutput(String lineToAdd) {
        if (lineToAdd == null) {
            return;
        }
        if (!output.isEmpty()) {
            output += "\n";
        }
        output += lineToAdd;
    }

    private void addPerforceDepotInfoForFiles() {
        if (!depotFilesToCheck.isEmpty()) {
            String filesListToCheck = "";
            for (String depotFileToCheck : depotFilesToCheck) {
                if (!filesListToCheck.isEmpty()) {
                    filesListToCheck += " ";
                }
                String fileVersion = StringUtils.isNotEmpty(lastSubmittedChangelist) ? "@" + lastSubmittedChangelist : "";
                filesListToCheck += format("%s%s", depotFileToCheck, fileVersion);
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
            Matcher infoMatcher = parseLine(depotFileChecked, depotFileInfo, depotFileInfoPattern);
            if (infoMatcher == null) {
                whereFilesToCheck.add(depotFileChecked);
                continue;
            }
            String depotMapping = infoMatcher.group(1);
            depotMappings.put(depotFileChecked, depotMapping);
            depotVersions.put(depotMapping, infoMatcher.group(2));
        }
    }

    private Matcher parseLine(String fileName, String line, Pattern patternToUse) {
        Matcher matcher = patternToUse.matcher(line);
        if (!matcher.find()) {
            log.warn("Expected {} to exist in perforce: unexpected response {} for p4 files command, using p4 where to get file path", fileName, line);
            return null;
        }
        return matcher;
    }

    private String createPerforceMinusFile(String minusDiffFile) {
        return format("--- [!!%s!!]\t[!!%s#0!!]", minusDiffFile, minusDiffFile);
    }

    private String createPerforceAddFile(String addDiffFile) {
        return "+++ [!!" + addDiffFile + "!!]\t" + diffDate;
    }

    public static void main(String[] args) {
        String diff = IOUtils.read(new File("/Users/dbiggs/Downloads/rb1030085.patch"));
        PerforceDiffToGitConverter converter = new PerforceDiffToGitConverter(new Git());
        String diffText = converter.convert(diff);
        System.out.println(diffText);

    }
}

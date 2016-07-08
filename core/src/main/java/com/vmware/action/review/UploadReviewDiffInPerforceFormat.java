package com.vmware.action.review;

import com.vmware.Perforce;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.DiffToUpload;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import java.io.ByteArrayInputStream;
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

@ActionDescription("Uploads a git diff in perforce format for the review. The parent ref used is defined by the parentBranch config property.")
public class UploadReviewDiffInPerforceFormat extends UploadReviewDiff {

    private static final Pattern depotFileInfoPattern = Pattern.compile("(.+)#(\\d+)");
    private static final Pattern whereFileInfoPattern = Pattern.compile("(//.+?)\\s+");
    private static final List<String> VALUES_TO_IGNORE = Arrays.asList("diff --git", "index ", "deleted file mode", "new file mode");
    private Perforce perforce;

    private List<String> depotFilesToCheck = new ArrayList<>();
    private List<String> whereFilesToCheck = new ArrayList<>();
    private Map<String, String> depotMappings = new HashMap<>();
    private Map<String, String> depotVersions = new HashMap<>();
    private String diffDate;
    private String lastDiffFile;

    public UploadReviewDiffInPerforceFormat(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        super.preprocess();
        this.perforce = serviceLocator.getPerforce();
    }

    protected DiffToUpload createReviewRequestDiff() {
        log.info("Converting git diff into a diff in perforce format against parent branch {}", config.parentBranch);
        clearValues();
        String reviewBoardVersion = reviewBoard.getVersion();
        boolean supportsDiffWithRenames = reviewBoardVersion.compareTo("1.7") >= 0;
        log.debug("Review board version: {}, Supports renames {}", reviewBoardVersion, supportsDiffWithRenames);

        DiffToUpload diff = new DiffToUpload();
        String mergeBase = git.mergeBase(config.trackingBranch, "HEAD");
        diff.path = convertGitDiffToPerforceDiff(git.diff(config.parentBranch, "HEAD", supportsDiffWithRenames));
        diff.parent_diff_path = convertGitDiffToPerforceDiff(git.diff(mergeBase, config.parentBranch, supportsDiffWithRenames));
        return diff;
    }

    private void clearValues() {
        depotMappings.clear();
        depotVersions.clear();
    }

    private byte[] convertGitDiffToPerforceDiff(byte[] gitDiff) {
        if (gitDiff == null) {
            return null;
        } else if (gitDiff.length == 0) {
            return new byte[0];
        }
        depotFilesToCheck.clear();
        whereFilesToCheck.clear();

        diffDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        List<String> diffLines = IOUtils.readLines(new ByteArrayInputStream(gitDiff));
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
            addWhereFileToCheckIfNeedd(addFile);
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
        } else if (!lineCanBeIgnored(diffLine)) {
            lineToAdd = diffLine;
        }
        return lineToAdd;
    }

    private String createFileRenameText(Iterator<String> linesIterator, String similarityIndex) {
        int similarityValue = Integer.parseInt(similarityIndex);
        String renameFromFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "rename from (.+)");
        String renameToFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "rename to (.+)");
        addDepotFileForCheckingIfNeeded(renameFromFile);
        addWhereFileToCheckIfNeedd(renameToFile);
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

    private void addWhereFileToCheckIfNeedd(String diffFile) {
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
            String filesListToCheck = StringUtils.appendWithDelimiter(" ", depotFilesToCheck, " " + perforce.getWorkingDirectory() + "/").trim();
            String depotFilesInfo = perforce.getFileInfo(filesListToCheck);
            parsePerforceOutput(depotFilesInfo, depotFilesToCheck, depotFileInfoPattern, false);
        }

        if (!whereFilesToCheck.isEmpty()) {
            String whereFilesListToCheck = StringUtils.appendWithDelimiter(" ", whereFilesToCheck, " " + perforce.getWorkingDirectory() + "/").trim();
            String whereFilesInfo = perforce.getWhereFileInfo(whereFilesListToCheck);
            parsePerforceOutput(whereFilesInfo, whereFilesToCheck, whereFileInfoPattern, true);
        }
    }

    private void parsePerforceOutput(String perforceOutput, List<String> sourceValues, Pattern patternToUse, boolean hardcodeVersion) {
        int counter = 0;
        for (String depotFileInfo : perforceOutput.split("\n")) {
            String depotFileChecked = sourceValues.get(counter++);
            Matcher infoMatcher = parseLine(depotFileInfo, patternToUse);
            String depotMapping = infoMatcher.group(1);
            depotMappings.put(depotFileChecked, depotMapping);
            depotVersions.put(depotMapping, hardcodeVersion ? "0" : infoMatcher.group(2));
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

    private boolean lineCanBeIgnored(String diffLine) {
        for (String value : VALUES_TO_IGNORE) {
            if (diffLine.startsWith(value)) {
                return true;
            }
        }
        return false;
    }
}

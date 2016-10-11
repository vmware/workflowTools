package com.vmware.scm.diff;

import com.vmware.scm.FileChange;
import com.vmware.scm.FileChangeType;
import com.vmware.scm.Perforce;
import com.vmware.scm.ScmType;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a perforce diff to a git diff.
 */
public class PerforceDiffToGitConverter {

    private static final String[] VALUES_TO_IGNORE = new String[]{"Moved from", "Moved to"};
    private List<String> depotPaths = new ArrayList<>();

    private String diffText;
    private List<FileChange> fileChanges;

    public String convert(String perforceDiff) {
        ListIterator<String> lineIter = Arrays.asList(perforceDiff.split("\n")).listIterator();
        StringBuilder builder = new StringBuilder("");
        Matcher minusMatcher = Pattern.compile("---\\s+.+\\s+(.+)#(\\d+)").matcher("");
        fileChanges = new ArrayList<>();
        while (lineIter.hasNext()) {
            String line = lineIter.next();
            minusMatcher.reset(line);
            if (StringUtils.textStartsWithValue(line, VALUES_TO_IGNORE)) {
                continue;
            }

            if (minusMatcher.find()) {
                FileChange fileChange = determineFileChange(lineIter, minusMatcher);
                fileChanges.add(fileChange);
                builder.append(fileChange.diffGitLine()).append("\n");
                builder.append(fileChange.createMinusPlusLines()).append("\n");
            } else {
                builder.append(line).append("\n");
            }
        }
        diffText = builder.toString();
        diffText = replacePathsWithLocalPaths(diffText);
        return diffText;
    }

    public String getDiffText() {
        return diffText;
    }

    public List<FileChange> getFileChanges() {
        return fileChanges;
    }

    private String replacePathsWithLocalPaths(String diffText) {
        for (String depotPath : depotPaths) {
            int slashIndexAfterDepotPath = StringUtils.indexOrNthOccurence(depotPath, "/", 5);
            if (slashIndexAfterDepotPath == -1) {
                throw new RuntimeException("Expected to find depot path of format //depot/cloudName/branchName/ in depot path " + depotPath);
            }
            String relativePath = depotPath.substring(slashIndexAfterDepotPath + 1);
            diffText = diffText.replace(depotPath, relativePath);
        }
        return diffText;
    }

    private FileChange determineFileChange(ListIterator<String> lineIter, Matcher minusMatcher) {
        String minusDepotPath = minusMatcher.group(1);
        FileChange fileChange = new FileChange(ScmType.git);
        int depotVersion = Integer.parseInt(minusMatcher.group(2));
        String plusDepotPath = MatcherUtils.singleMatchExpected(lineIter.next(), "\\+\\+\\+\\s+(.+?)\\s+");
        String linesLeft = determineLinesLeftAfterChange(lineIter);
        fileChange.setFileVersion(depotVersion);
        fileChange.setChangeType(determineChangeType(minusDepotPath, plusDepotPath, depotVersion, linesLeft));
        fileChange.addFileAffected(0, minusDepotPath);
        fileChange.addFileAffected(1, plusDepotPath);
        depotPaths.add(minusDepotPath);
        if (!plusDepotPath.equals(minusDepotPath)) {
            depotPaths.add(plusDepotPath);
        }
        return fileChange;
    }

    private String determineLinesLeftAfterChange(ListIterator<String> lineIter) {
        String addedLines = null;
        if (lineIter.hasNext()) {
            addedLines = MatcherUtils.singleMatch(lineIter.next(), "@@\\s+-\\d+,\\d+\\s+\\+(\\d+,\\d+)\\s+@@");
            lineIter.previous();
        }
        return addedLines;
    }

    private FileChangeType determineChangeType(String minusDepotPath, String plusDepotPath, int depotVersion, String linesLeft) {
        if (!minusDepotPath.equals(plusDepotPath)) {
            return FileChangeType.renamed;
        } else if ("0,0".equals(linesLeft)) {
            return FileChangeType.deleted;
        } else if (depotVersion == 0) {
            return FileChangeType.added;
        } else {
            return FileChangeType.modified;
        }
    }
}

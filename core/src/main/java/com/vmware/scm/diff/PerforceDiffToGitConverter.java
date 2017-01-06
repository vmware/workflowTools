package com.vmware.scm.diff;

import com.vmware.scm.FileChange;
import com.vmware.scm.FileChangeType;
import com.vmware.scm.ScmType;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
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
        String[] linesInfo = determineLinesInfoForChange(lineIter);
        fileChange.setFileVersion(depotVersion);
        fileChange.setChangeType(determineChangeType(minusDepotPath, plusDepotPath, depotVersion, linesInfo));
        if (fileChange.getChangeType() == FileChangeType.added) {
            fileChange.setFileMode("100644");
        }
        fileChange.addFileAffected(0, minusDepotPath);
        fileChange.addFileAffected(1, plusDepotPath);
        depotPaths.add(minusDepotPath);
        if (!plusDepotPath.equals(minusDepotPath)) {
            depotPaths.add(plusDepotPath);
        }
        return fileChange;
    }

    private String[] determineLinesInfoForChange(ListIterator<String> lineIter) {
        if (lineIter.hasNext()) {
            Matcher infoMatcher = Pattern.compile("@@\\s+-(\\d+,\\d+)\\s+\\+(\\d+,\\d+)\\s+@@").matcher(lineIter.next());
            lineIter.previous();
            if (infoMatcher.find()) {
                return new String[] {infoMatcher.group(1), infoMatcher.group(2)};
            }
        }
        return null;
    }

    private FileChangeType determineChangeType(String minusDepotPath, String plusDepotPath, int depotVersion, String[] linesInfo) {
        if (!minusDepotPath.equals(plusDepotPath)) {
            return FileChangeType.renamed;
        } else if (linesInfo != null && "0,0".equals(linesInfo[1])) {
            return FileChangeType.deleted;
        } else if (depotVersion == 0) {
            return FileChangeType.added;
        } else if (depotVersion == 1 && linesInfo != null && "0,0".equals(linesInfo[0])) {
            return FileChangeType.added;
        } else {
            return FileChangeType.modified;
        }
    }
}

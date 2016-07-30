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


    private Perforce perforce;

    public PerforceDiffToGitConverter(Perforce perforce) {
        this.perforce = perforce;
    }

    public String convert(String perforceDiff) {
        ListIterator<String> lineIter = Arrays.asList(perforceDiff.split("\n")).listIterator();
        StringBuilder builder = new StringBuilder("");
        Matcher minusMatcher = Pattern.compile("---\\s+.+\\s+(.+)#(\\d+)").matcher("");
        while (lineIter.hasNext()) {
            String line = lineIter.next();
            minusMatcher.reset(line);
            if (StringUtils.textStartsWithValue(line, VALUES_TO_IGNORE)) {
                continue;
            }

            if (minusMatcher.find()) {
                FileChange fileChange = determineFileChange(lineIter, minusMatcher);
                builder.append(fileChange.diffGitLine()).append("\n");
                builder.append(fileChange.createMinusPlusLines()).append("\n");
            } else {
                builder.append(line).append("\n");
            }
        }
        String diffText = builder.toString();
        diffText = replacePathsWithLocalPaths(diffText);
        return diffText;
    }

    private String replacePathsWithLocalPaths(String diffText) {
        Map<String, String> depotMappings = perforce.getWhereLocalFileInfo(depotPaths);
        String clientDirectory = perforce.getWorkingDirectory().getPath();
        for (String depotMapping : depotMappings.keySet()) {
            String localPath = depotMappings.get(depotMapping);
            if (!localPath.startsWith(clientDirectory)) {
                throw new RuntimeException("Expected local path " + localPath + " to start with client directory " + clientDirectory);
            }
            String relativePath = localPath.substring(clientDirectory.length() + 1);
            diffText = diffText.replace(depotMapping, relativePath);
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

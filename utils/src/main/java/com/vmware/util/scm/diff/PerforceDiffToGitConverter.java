package com.vmware.util.scm.diff;

import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.FileChangeType;
import com.vmware.util.scm.Git;
import com.vmware.util.scm.ScmType;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Converts a perforce diff to a git diff.
 */
public class PerforceDiffToGitConverter implements DiffConverter {

    private static final String NON_EXISTENT_INDEX_IN_GIT = "0000000000000000000000000000000000000000";
    private static final String[] VALUES_TO_IGNORE = new String[]{"Moved from", "Moved to"};
    private Set<String> depotPaths = new LinkedHashSet<>();

    private String diffText;
    private List<FileChange> fileChanges;

    private Git git;

    public PerforceDiffToGitConverter(Git git) {
        this.git = git;
    }

    public String convert(String perforceDiff) {
        ListIterator<String> lineIter = Arrays.asList(perforceDiff.split("\n")).listIterator();
        StringBuilder builder = new StringBuilder("");
        Matcher minusMatcher = Pattern.compile("---\\s+.+\\t(.+)#(\\d+)").matcher("");
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
                if (fileChange.getChangeType() == FileChangeType.deleted) {
                    String fileIndex = git.hashObject(new File(git.fullPath(fileChange.getLastFileAffected())));
                    builder.append(format("index %s..%s", fileIndex, NON_EXISTENT_INDEX_IN_GIT)).append("\n");
                }
                builder.append(fileChange.createMinusPlusLines()).append("\n");
            } else {
                builder.append(line).append("\n");
            }
        }
        diffText = builder.toString();
        return diffText;
    }

    @Override
    public byte[] convertAsBytes(String diffData) {
        String convertedData = convert(diffData);
        return convertedData != null ? convertedData.getBytes(Charset.forName("UTF-8")) : null;
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

    private String relativePath(String depotPath) {
        int slashIndexAfterDepotPath = StringUtils.indexOrNthOccurence(depotPath, "/", 5);
        if (slashIndexAfterDepotPath == -1) {
            throw new RuntimeException("Expected to find depot path of format //depot/cloudName/branchName/ in depot path " + depotPath);
        }
        return depotPath.substring(slashIndexAfterDepotPath + 1);
    }

    private FileChange determineFileChange(ListIterator<String> lineIter, Matcher minusMatcher) {
        String minusDepotPath = minusMatcher.group(1);
        if (!minusDepotPath.startsWith("//")) {
            throw new RuntimeException("Expected minus depot path to start with //\n" + minusDepotPath);
        }
        FileChange fileChange = new FileChange(ScmType.git);
        int depotVersion = Integer.parseInt(minusMatcher.group(2));
        String plusDepotPath = MatcherUtils.singleMatchExpected(lineIter.next(), "\\+\\+\\+\\s+(.+?)\\t+");
        String[] linesInfo = determineLinesInfoForChange(lineIter);
        fileChange.setFileVersion(depotVersion);
        fileChange.setChangeType(determineChangeType(minusDepotPath, plusDepotPath, depotVersion, linesInfo));
        if (fileChange.getChangeType() == FileChangeType.added) {
            fileChange.setFileMode("100644");
        }
        if (!plusDepotPath.equals(minusDepotPath)) {
            if (!plusDepotPath.startsWith("//")) {
                throw new RuntimeException("Expected plus depot path to start with //\n" + plusDepotPath);
            }
        }
        fileChange.addFileAffected(0, relativePath(minusDepotPath));
        fileChange.addFileAffected(1, relativePath(plusDepotPath));
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

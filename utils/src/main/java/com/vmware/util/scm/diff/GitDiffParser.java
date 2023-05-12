package com.vmware.util.scm.diff;

import com.vmware.util.MatcherUtils;
import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.FileChangeType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.vmware.util.scm.ScmType.git;

public class GitDiffParser implements DiffConverter{

    private List<FileChange> fileChanges;

    private String lastDiffFile;

    @Override
    public String convert(String diffData) {
        if (diffData == null) {
            return null;
        } else if (diffData.isEmpty()) {
            return "";
        }
        fileChanges = new ArrayList<>();
        List<String> diffLines = Arrays.asList(diffData.split("\n"));
        lastDiffFile = "";
        Iterator<String> linesIterator = diffLines.iterator();
        while (linesIterator.hasNext()) {
            parseFileChanges(linesIterator);
        }
        return diffData;
    }

    @Override
    public byte[] convertAsBytes(String diffData) {
        return convert(diffData).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public List<FileChange> getFileChanges() {
        return fileChanges;
    }

    private void parseFileChanges(Iterator<String> linesIterator) {
        String diffLine = linesIterator.next();
        String minusDiffFile = MatcherUtils.singleMatch(diffLine, "---\\s+a/(.+)");
        String addDiffFile = MatcherUtils.singleMatch(diffLine, "\\+\\+\\+\\s+b/(.+)");
        FileChange fileChange = null;
        if (diffLine.startsWith("--- /dev/null")) {
            String addFile = MatcherUtils.singleMatchExpected(linesIterator.next(), "\\+\\+\\+\\s+b/(.+)");
            fileChange = new FileChange(git, FileChangeType.added, addFile);
        } else if (diffLine.startsWith("+++ /dev/null")) {
            fileChange = new FileChange(git, FileChangeType.deleted, lastDiffFile);
        } else if (minusDiffFile != null) {
            lastDiffFile = minusDiffFile;
        } else if (addDiffFile != null) {
            fileChange = new FileChange(git, FileChangeType.modified, addDiffFile);
        }
        if (fileChange != null) {
            fileChanges.add(fileChange);
        }
    }
}

package com.vmware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vmware.util.scm.FileChange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiffUtils {
    private static Logger log = LoggerFactory.getLogger(DiffUtils.class);

    public static String compareDiffContent(String firstDiff, String secondDiff, String firstDiffLabel, String secondDiffLabel) {
        log.debug("Converting {} diff to file map", firstDiffLabel);
        Map<String, String> firstDiffMap = convertDiffToMap(firstDiff);
        log.debug("Converting {} diff to file map", secondDiffLabel);
        Map<String, String> secondDiffMap = convertDiffToMap(secondDiff);
        log.debug("Comparing files individually");
        return compareDiff(firstDiffMap, secondDiffMap, firstDiffLabel, secondDiffLabel);
    }

    private static Map<String, String> convertDiffToMap(String diff) {
        Matcher diffLineMatcher = Pattern.compile("diff --first\\s+.+b/(.+)").matcher("");
        String fileName = null;
        StringBuilder fileDiffBuilder = new StringBuilder();
        Map<String, String> fileDiffs = new HashMap<>();
        for (String diffLine : diff.split("\n")) {
            if (diffLine.startsWith("---") || diffLine.startsWith("+++")) {
                continue;
            }
            diffLineMatcher.reset(diffLine);
            if (diffLineMatcher.find()) {
                if (fileName != null) {
                    fileDiffs.put(fileName, fileDiffBuilder.toString());
                }
                fileName = diffLineMatcher.group(1);
                fileDiffBuilder = new StringBuilder(diffLine);
            } else if (fileDiffBuilder.length() > 0) {
                fileDiffBuilder.append("\n").append(diffLine);
            } else {
                fileDiffBuilder.append(diffLine);
            }
        }
        if (fileName != null) {
            fileDiffs.put(fileName, fileDiffBuilder.toString());
        }
        return fileDiffs;
    }

    private static String compareDiff(Map<String, String> firstDiff, Map<String, String> secondDiff, String firstDiffLabel, String secondDiffLabel) {
        if (!firstDiff.keySet().equals(secondDiff.keySet())) {
            Set<String> fileChangesNotInSecondDiff = new HashSet<>(firstDiff.keySet());
            fileChangesNotInSecondDiff.removeAll(secondDiff.keySet());
            Set<String> fileChangesNotInFirstDiff = new HashSet<>(secondDiff.keySet());
            fileChangesNotInFirstDiff.removeAll(firstDiff.keySet());
            String errorText = "File list for diff is different";
            if (!fileChangesNotInSecondDiff.isEmpty()) {
                errorText += "\nFiles present in " + firstDiffLabel + " but missing in " + secondDiffLabel + " " + fileChangesNotInSecondDiff.toString();
            }
            if (!fileChangesNotInFirstDiff.isEmpty()) {
                errorText += "\nFiles present in " + secondDiffLabel + " but missing in " + firstDiffLabel + " " + fileChangesNotInFirstDiff.toString();
            }
            return errorText;
        }
        for (String firstDiffFile : firstDiff.keySet()) {
            String firstDiffFileText = firstDiff.get(firstDiffFile);
            String secondDiffFileText = secondDiff.get(firstDiffFile);
            String reasonForNotMatching = compareDiffText(firstDiffFileText, secondDiffFileText, firstDiffLabel, secondDiffLabel);
            if (reasonForNotMatching != null) {
                return firstDiffFile + " did not match\n\n" + reasonForNotMatching;
            }
        }
        return null;
    }

    private static String compareDiffText(String firstDiff, String secondDiff, String firstDiffLabel, String secondDiffLabel) {
        if (StringUtils.equals(firstDiff, secondDiff)) {
            return null;
        }

        String[] firstDiffLines = firstDiff.split("\n");
        String[] secondDiffLines = secondDiff.split("\n");
        String output = "";
        if (firstDiffLines.length != secondDiffLines.length
                && !firstDiff.contains("+++ " + FileChange.NON_EXISTENT_FILE_IN_GIT)) {
            output = "lines count mismatch, " + secondDiffLabel + ": " + secondDiffLines.length + " " + secondDiffLabel + ": " + firstDiffLines.length + "\n";
        }
        Iterator<String> firstDiffIterator = Arrays.asList(firstDiffLines).iterator();
        Iterator<String> secondDiffIterator = Arrays.asList(secondDiffLines).iterator();

        int lineCount = 0;
        String firstDiffLineAfterMoving = null;
        List<String> secondLines = new ArrayList<>();
        List<String> firstLines = new ArrayList<>();
        while (secondDiffIterator.hasNext()) {
            String firstDiffLine;
            if (firstDiffLineAfterMoving != null) {
                firstDiffLine = firstDiffLineAfterMoving;
                firstDiffLineAfterMoving = null;
            } else if (firstDiffIterator.hasNext()) {
                firstDiffLine = firstDiffIterator.next();
            } else {
                return output + "same until extra lines in " + firstDiffLabel + " diff";
            }

            lineCount++;
            String secondDiffLine = secondDiffIterator.next();
            addDiffLine(secondLines, secondDiffLine);
            addDiffLine(firstLines, firstDiffLine);
            if (!StringUtils.equals(secondDiffLine, firstDiffLine)) {
                addFollowingLines(secondLines, secondDiffIterator);
                addFollowingLines(firstLines, firstDiffIterator);
                String lineDifference = "DIFF DIFFERENCE \n(" + secondDiffLabel + " line " + lineCount + ")\n"
                        + secondDiffLine + "\n(" + firstDiffLabel + " line " + lineCount + ")\n" + firstDiffLine;
                String secondDiffText = "\n**** " + secondDiffLabel.toUpperCase() + " DIFF SAMPLE ****\n" + StringUtils.join(secondLines, "\n");
                String firstDiffText = "\n**** " + firstDiffLabel.toUpperCase() + " DIFF SAMPLE ****\n" + StringUtils.join(firstLines, "\n");
                return lineDifference + "\n" + secondDiffText + "\n" + firstDiffText;
            }
            if (firstDiffLine.startsWith("+++ " + FileChange.NON_EXISTENT_FILE_IN_GIT)) {
                log.info("Ignoring delete file lines in {} diff as the {} diff will not have those", firstDiffLabel, secondDiffLabel);
                firstDiffLineAfterMoving = moveIteratorUntilLineStartsWith(firstDiffIterator, "diff --first");
            }
        }
        return "failed to find expected difference between " + firstDiffLabel + " and " + secondDiffLabel + " diff";
    }

    private static void addFollowingLines(List<String> lines, Iterator<String> diffIterator) {
        int count = 0;
        while (count++ < 10 && diffIterator.hasNext()) {
            lines.add(diffIterator.next());
        }
    }

    private static void addDiffLine(List<String> lines, String line) {
        if (lines.size() >= 10) {
            lines.remove(0);
        }
        lines.add(line);
    }

    private static String moveIteratorUntilLineStartsWith(Iterator<String> iterator, String valueToMatch) {
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.startsWith(valueToMatch)) {
                return line;
            }
        }
        return null;
    }

}

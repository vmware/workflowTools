package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.section.CommitStatsConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@ActionDescription(value = "Generates stats for git commits.", ignoreConfigValuesInSuperclass = true)
public class GenerateGitCommitStats extends BaseAction {

    private static final List<String> sizeNames = Arrays.asList("small", "medium", "large", "xlarge", "xxlarge", "xxxlarge", "giant");

    public GenerateGitCommitStats(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        CommitStatsConfig statsConfig = this.statsConfig;
        if (statsConfig.fileCountRanges.length != statsConfig.lineCountRanges.length) {
            return "fileCountRanges " + Arrays.toString(statsConfig.fileCountRanges)
                    + " must be the same length as lineCountRanges " + Arrays.toString(statsConfig.lineCountRanges);
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        Date oldestDateToCheckAgainst = new Date(new Date().getTime() - TimeUnit.DAYS.toMillis(statsConfig.lastNumberOfDaysForStats));
        log.info("Computing stats against all git commits newer than {} ({} days ago)",
                oldestDateToCheckAgainst.toString(), statsConfig.lastNumberOfDaysForStats);

        String commitText = git.commitText(0);
        ReviewRequestDraft draft = new ReviewRequestDraft(commitText, commitConfig);
        TitledHashMap<String, Integer> totalCounts = new TitledHashMap<>("total");
        List<TitledHashMap<String, Integer>> countRanges = new ArrayList<>();
        Map<String, List<String>> commitSummariesForAuthor = new HashMap<>();
        for (int i = 0; i < (statsConfig.fileCountRanges.length); i ++) {
            log.info("{}: {} max file changes, {} max line changes", sizeNames.get(i),
                    statsConfig.fileCountRanges[i], statsConfig.lineCountRanges[i]);
            countRanges.add(new TitledHashMap<>(sizeNames.get(i)));
        }

        String spillOverSize = sizeNames.get(statsConfig.fileCountRanges.length);
        log.info("{}: everything bigger", spillOverSize);
        countRanges.add(new TitledHashMap<>(spillOverSize));

        TitledHashMap<String, Integer> trivialAuthorCounts = new TitledHashMap<>("trivial");
        TitledHashMap<String, Integer> noBugNumberCounts = new TitledHashMap<>("noBugNumber");
        TitledHashMap<String, Integer> onelineTestingDoneCounts = new TitledHashMap<>("oneliners");
        TitledHashMap<String, Integer> shortTestingDoneCounts = new TitledHashMap<>("short description");
        int numberOfCommitsChecked = 0;
        while (draft.commitDate != null && oldestDateToCheckAgainst.before(draft.commitDate)) {
            String authorEmail = draft.authorEmail;
            if (StringUtils.isBlank(authorEmail)) {
                commitText = git.commitText(++numberOfCommitsChecked);
                draft = new ReviewRequestDraft(commitText, commitConfig);
                continue;
            }
            incrementCount(authorEmail, totalCounts);
            if (draft.isTrivialCommit(commitConfig.trivialReviewerLabel)) {
                incrementCount(authorEmail, trivialAuthorCounts);
            }

            for (int i = 0; i < statsConfig.fileCountRanges.length; i ++) {
                boolean isCommitSmaller = draft.isCommitSmallerThan(statsConfig.fileCountRanges[i], statsConfig.lineCountRanges[i]);
                if (isCommitSmaller) {
                    incrementCount(authorEmail, countRanges.get(i));
                    break; // should only be smaller than one value
                } else if (i == statsConfig.fileCountRanges.length -1) { // increment if commit is larger than last value
                    incrementCount(authorEmail, countRanges.get(i + 1));
                }
            }

            if (!draft.hasBugNumber(commitConfig.noBugNumberLabel)) {
                incrementCount(authorEmail, noBugNumberCounts);
            }
            if (StringUtils.isBlank(draft.testingDone) || !draft.testingDone.contains("\n")) {
                incrementCount(authorEmail, onelineTestingDoneCounts);
            }
            if (StringUtils.isBlank(draft.testingDone) || draft.testingDone.length() < 40) {
                incrementCount(authorEmail, shortTestingDoneCounts);
            }
            if ("all".equalsIgnoreCase(statsConfig.authorsForCommits) || draft.matchesAuthor(statsConfig.authorsForCommits)) {
                if (!commitSummariesForAuthor.containsKey(draft.authorEmail)) {
                    commitSummariesForAuthor.put(draft.authorEmail, new ArrayList<>());
                }
                commitSummariesForAuthor.get(draft.authorEmail).add(draft.summaryInfo(statsConfig.maxSummaryLength));
            }
            commitText = git.commitText(++numberOfCommitsChecked);
            draft = new ReviewRequestDraft(commitText, commitConfig);
        }
        log.info("{} commits checked", numberOfCommitsChecked);

        countRanges.add(0, totalCounts);
        printResults(countRanges, "Total counts");
        printResults(Collections.singletonList(trivialAuthorCounts), "Trivial commit counts");
        printResults(Collections.singletonList(noBugNumberCounts), "No bug number commit counts");
        printResults(Arrays.asList(shortTestingDoneCounts, onelineTestingDoneCounts), "Short Testing done counts");

        if (!commitSummariesForAuthor.isEmpty()) {
            printCommitSummaries(commitSummariesForAuthor);
        }
    }

    private void printCommitSummaries(Map<String, List<String>> commitSummariesForAuthor) {
        List<String> sortedAuthors = commitSummariesForAuthor.keySet().stream().sorted().collect(Collectors.toList());
        for (String authorEmail : sortedAuthors) {
            Padder authorPadder = new Padder("Commits for {}", authorEmail);
            authorPadder.infoTitle();
            for (String summary : commitSummariesForAuthor.get(authorEmail)) {
                log.info(summary);
            }
            authorPadder.infoTitle();
        }
    }

    private void incrementCount(String authorEmail, Map<String, Integer> counts) {
        if (!counts.containsKey(authorEmail)) {
            counts.put(authorEmail, 1);
        } else {
            counts.put(authorEmail, counts.get(authorEmail) + 1);
        }
    }

    private void printResults(List<TitledHashMap<String, Integer>> stats, String overallTitle) {
        Comparator<Map.Entry<String, Integer>> countComparator = Comparator.comparing(new Function<Map.Entry<String,Integer>, Integer>() {
            @Override
            public Integer apply(Map.Entry<String,Integer> entry) {
                return entry.getValue();
            }
        }).reversed();


            Padder titlePadder = new Padder(overallTitle);
            titlePadder.infoTitle();
            TitledHashMap<String, Integer> primaryStat = stats.get(0);
            primaryStat.entrySet().stream().sorted(countComparator)
                    .forEachOrdered(entry -> log.info("{}: {}{} {}", entry.getKey(), stats.size() > 1 ? primaryStat.getTitle() + ": " : "",
                            entry.getValue(), additionalStats(stats, entry.getKey())));
            titlePadder.infoTitle();
    }

    private String additionalStats(List<TitledHashMap<String, Integer>> statsList, String key) {
        String additionalStatLine = "";
        for (int i = 1; i < statsList.size(); i++) {
            TitledHashMap<String, Integer> stat = statsList.get(i);
            if (!stat.containsKey(key)) {
                additionalStatLine = StringUtils.appendWithDelimiter(additionalStatLine, stat.getTitle() +  ": 0", ", ");
            } else {
                additionalStatLine = StringUtils.appendWithDelimiter(additionalStatLine, stat.getTitle() + ": " + stat.get(key), ", ");
            }
        }
        return additionalStatLine;
    }

    private class TitledHashMap<K,V> extends HashMap<K,V> {
        private String title;

        public TitledHashMap(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}

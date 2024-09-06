package com.vmware.reviewboard.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.vmware.BuildStatus;
import com.vmware.IssueInfo;
import com.vmware.config.section.CommitConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jira.domain.Issue;
import com.vmware.util.DateUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.util.StringUtils.appendCsvValue;
import static com.vmware.util.StringUtils.isEmpty;
import static com.vmware.util.StringUtils.isNotEmpty;
import static com.vmware.util.StringUtils.pluralize;
import static com.vmware.util.StringUtils.truncateStringIfNeeded;
import static com.vmware.util.UrlUtils.addRelativePaths;
import static com.vmware.util.logging.LogLevel.DEBUG;

public class ReviewRequestDraft extends BaseEntity {
    @Expose(serialize = false, deserialize = false)
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Expose(serialize = false, deserialize = false)
    private DynamicLogger dynamicLog = new DynamicLogger(log);

    @Expose(serialize = false)
    public String id;
    @Expose(serialize = false, deserialize = false)
    public ReviewRequest reviewRequest;
    @Expose(serialize = false, deserialize = false)
    public RepoType repoType;
    public String summary = "";
    public String description = "";
    @SerializedName("testing_done")
    public String testingDone = "";
    @Expose
    @SerializedName("bugs_closed")
    @JsonAdapter(StringArrayDeserializer.class)
    public String bugNumbers = "";
    @Expose
    @SerializedName("target_people")
    @JsonAdapter(LinkArrayDeserializer.class)
    public String reviewedBy = "";
    @Expose(serialize = false, deserialize = false)
    public String shipItReviewers = "";
    @SerializedName("target_groups")
    @JsonAdapter(LinkArrayDeserializer.class)
    public String targetGroups;
    @SerializedName("depends_on")
    @JsonAdapter(RequestLinksArrayDeserializer.class)
    public String dependsOnRequests;
    @Expose(deserialize = false)
    @SerializedName("description_text_type")
    public String descriptionTextType;
    @Expose(deserialize = false)
    @SerializedName("testing_done_text_type")
    public String testingDoneTextType;
    @Expose(deserialize = false)
    @SerializedName("changedescription")
    public String changeDescription;

    @Expose(serialize = false, deserialize = false)
    public List<JobBuild> jobBuilds = new ArrayList<>();
    @Expose(serialize = false, deserialize = false)
    public Integer selectedBuild;
    @Expose(serialize = false, deserialize = false)
    public List<IssueInfo> issues = new ArrayList<>();
    public String branch = "";
    @Expose(serialize = false, deserialize = false)
    public String[] mergeToValues = new String[0];
    @Expose(serialize = false, deserialize = false)
    public String approvedBy;
    @Expose(serialize = false, deserialize = false)
    public String pipeline;
    @Expose(serialize = false, deserialize = false)
    public String draftPatchData;
    @Expose(serialize = false, deserialize = false)
    public Set<String> extraTargetGroupsToAdd = new TreeSet<>();

    /**
     * Boolean object as review board 1.7 treats any value for isPublic as true.
     * To keep review request private, send null for isPublic
     */
    @SerializedName("public")
    public Boolean isPublic;

    /**
     * Set to true to publish review without sending emails to reviewers
     */
    public Boolean trivial;

    /**
     * Id used in review request for commit
     */
    @SerializedName("commit_id")
    public String commitId;

    @Expose(serialize = false, deserialize = false)
    public Boolean jenkinsBuildsAreSuccessful;

    @Expose(serialize = false, deserialize = false)
    public Boolean buildwebBuildsAreSuccessful;

    @Expose(serialize = false, deserialize = false)
    public String perforceChangelistId;

    @Expose(serialize = false, deserialize = false)
    public String commitHash;

    @Expose(serialize = false, deserialize = false)
    public String authorName;

    @Expose(serialize = false, deserialize = false)
    public String authorEmail;

    @Expose(serialize = false, deserialize = false)
    public Date commitDate;

    @Expose(serialize = false, deserialize = false)
    public int filesChanged;

    @Expose(serialize = false, deserialize = false)
    public int lineInsertions;

    @Expose(serialize = false, deserialize = false)
    public int lineDeletions;

    @Expose(serialize = false, deserialize = false)
    public String requestUrl;

    @Expose(serialize = false, deserialize = false)
    private MergeRequest gitlabMergeRequest;

    @Expose(serialize = false, deserialize = false)
    private PullRequest githubPullRequest;

    public ReviewRequestDraft() {}

    public ReviewRequestDraft(final String summary, final String description, final String testingDone,
                              final String bugNumbers, final String reviewedBy, String targetGroups, String branch) {
        this.summary = summary;
        this.description = description;
        this.testingDone = testingDone;
        this.bugNumbers = bugNumbers;
        this.reviewedBy = reviewedBy;
        this.targetGroups = targetGroups;
        this.branch = branch;
    }

    public ReviewRequestDraft(final String commitText, CommitConfig commitConfig) {
        fillValuesFromCommitText(commitText, commitConfig);
    }

    public static ReviewRequestDraft aDraftForPublishingAReview(String changeDescription, boolean trivial) {
        ReviewRequestDraft draft = new ReviewRequestDraft(null,null,null,null,null,null,null);
        draft.isPublic = true;
        draft.changeDescription = changeDescription;
        if (trivial) {
            draft.trivial = true;
        }
        return draft;
    }

    public void fillValuesFromCommitText(String commitText, CommitConfig commitConfig) {
        if (isEmpty(commitText)) {
            log.warn("Text is blank, can't extract commit values!");
            return;
        }
        this.commitId = parseSingleLineFromText(commitText, "commit\\s+(\\w+)", "Git commit id", DEBUG);
        this.perforceChangelistId = parseSingleLineFromText(commitText, "^Change\\s+(\\d+)", "Perforce Changelist Id", DEBUG);
        if (isNotEmpty(perforceChangelistId)) {
            log.debug("Matched first line of perforce changelist, id was {}", perforceChangelistId);
            commitText = commitText.substring(commitText.indexOf('\n')).trim();
        } else {
            this.perforceChangelistId = parseSingleLineFromText(commitText, "\\[git-p4:\\s+depot-paths.+?change\\s+=\\s+(\\d+)\\]", "Git P4 Changelist Id", DEBUG);
            if (isEmpty(this.perforceChangelistId)) {
                this.perforceChangelistId = parseSingleLineFromText(commitText, "^\\s*Change:\\s*(\\d+)", "Git Fusion Changelist Id", DEBUG);
            }
        }

        this.commitHash = parseSingleLineFromText(commitText, "^commit\\s+(\\w+)", "Git Commit Hash", DEBUG);
        this.authorName = parseSingleLineFromText(commitText, "^Author:\\s+(.+) <", "Git Commit Author Name", DEBUG);
        this.authorEmail = parseSingleLineFromText(commitText, "^Author:.+<(.+)>", "Git Commit Author Email", DEBUG);
        String commitDateAsString = parseSingleLineFromText(commitText, "^Date:\\s+(.+)", "Git Commit Date", DEBUG);
        if (isNotEmpty(commitDateAsString)) {
            commitText = commitText.substring(commitText.indexOf(commitDateAsString) + commitDateAsString.length());
            this.commitDate = DateUtils.parseDate(commitDateAsString);
        }

        while (commitText.startsWith("\n")) {
            commitText = commitText.substring(1);
        }

        String description = parseMultilineFromText(commitText, commitConfig.generateDescriptionPattern(), "Description");
        int summaryIndex = commitText.contains("\n") ? commitText.indexOf("\n") : commitText.length() - 1;
        String summary = commitText.substring(0, summaryIndex);
        description = description.length() < summary.length() + 1 ? "" : description.substring(summary.length() + 1);
        if (description.length() > 0 && description.charAt(0) == '\n') {
            description = description.substring(1);
        }
        String testingDoneSection = parseMultilineFromText(commitText, commitConfig.generateTestingDonePattern(), "Testing Done");
        String buildUrlsPattern = commitConfig.generateBuildUrlsPattern();
        this.id = parseSingleLineFromText(commitText, commitConfig.generateReviewUrlPattern(), "Review number");
        this.description = description;
        this.summary = summary;
        this.testingDone = stripJobBuildsFromTestingDone(testingDoneSection, buildUrlsPattern);
        this.jobBuilds.clear();
        this.jobBuilds.addAll(generateJobBuildsList(testingDoneSection, buildWithResultPattern(buildUrlsPattern)));
        this.jobBuilds.addAll(generateJobBuildsList(testingDoneSection, buildPattern(buildUrlsPattern)));
        this.bugNumbers = parseSingleLineFromText(commitText, commitConfig.generateBugNumberPattern(), "Bug Number");
        this.reviewedBy = parseSingleLineFromText(commitText, commitConfig.generateReviewedByPattern(), "Reviewers");
        this.approvedBy = parseSingleLineFromText(commitText, commitConfig.generateApprovedByPattern(), "Approved by");
        this.pipeline = parseSingleLineFromText(commitText, commitConfig.generatePipelinePattern(), "Run Pipeline");
        this.mergeToValues = parseRepeatingSingleLineFromText(commitText, commitConfig.generateMergeToPattern(), "Merge To");

        this.filesChanged = parseValueFromText(commitText, "(\\d+) files* changed", "Files Changed");
        this.lineInsertions = parseValueFromText(commitText, ".+?(\\d+) insertions*\\(\\+\\)", "Line Insertions");
        this.lineDeletions = parseValueFromText(commitText, ".+?(\\d+) deletions*\\(-\\)", "Lines Deleted");
    }

    public boolean hasReviewNumber() {
        return StringUtils.isInteger(id);
    }

    public boolean hasBugNumber(String noBugNumberLabel) {
        return isNotEmpty(bugNumbers) && !bugNumbers.equals(noBugNumberLabel);
    }

    public String topic() {
        return summary != null && summary.contains(":") ? summary.substring(0, summary.indexOf(":")) : null;
    }

    public String summaryWithoutTopic() {
        return summary != null && summary.contains(":") ? summary.substring(summary.indexOf(":") + 1).trim() : summary;
    }


    public boolean isTrivialCommit(String trivialReviewerLabel) {
        return reviewedBy.equals(trivialReviewerLabel);
    }

    public String fullTestingDoneSectionWithoutJobResults() {
        return fullTestingDoneSection(false);
    }

    private String fullTestingDoneSection(boolean includeResults) {
        StringBuilder sectionText = new StringBuilder(this.testingDone);
        for (JobBuild build : jobBuilds) {
            sectionText.append("\n").append(build.details(includeResults));
        }
        return sectionText.toString();
    }

    public JobBuild getMatchingJobBuild(Job job) {
        for (JobBuild build : jobBuilds) {
            if (StringUtils.equals(job.buildDisplayName, build.name) && build.url.startsWith(job.url)) {
                return build;
            }
        }
        return null;
    }

    private String stripJobBuildsFromTestingDone(String testingDone, String buildUrlsPattern) {
        String updatedSection = testingDone.replaceAll(buildWithResultPattern(buildUrlsPattern), "").trim();
        updatedSection = updatedSection.replaceAll(buildPattern(buildUrlsPattern), "").trim();
        return updatedSection;
    }

    private String buildPattern(String buildUrlsPattern) {
        return "(.+)\\s" + buildUrlsPattern;
    }

    private String buildWithResultPattern(String buildUrlsPattern) {
        return buildPattern(buildUrlsPattern) + "\\s+" + BuildStatus.generateResultPattern();
    }

    private List<JobBuild> generateJobBuildsList(String text, String jobUrlPattern) {
        Matcher buildMatcher = Pattern.compile("^" + jobUrlPattern + "\\s*$", Pattern.MULTILINE).matcher(text);
        List<JobBuild> jobBuilds = new ArrayList<>();
        while (buildMatcher.find()) {
            String buildDisplayName = buildMatcher.group(1);
            String buildUrl = buildMatcher.group(2);
            BuildStatus buildStatus = buildMatcher.groupCount() > 2 ? BuildStatus.valueOf(buildMatcher.group(3)) : null;
            jobBuilds.add(new JobBuild(buildDisplayName, buildUrl, buildStatus));
        }
        return jobBuilds;
    }

    public void setIssues(List<IssueInfo> issues, String noBugNumberLabel, boolean useLinkedBugzillaNumber) {
        this.issues.clear();
        this.issues.addAll(issues);

        this.bugNumbers = "";
        for (IssueInfo issue : issues) {
            if (issue == Issue.noBugNumber) {
                bugNumbers = appendCsvValue(bugNumbers, noBugNumberLabel);
            } else {
                String idToUse = useLinkedBugzillaNumber && StringUtils.isNotBlank(issue.getLinkedBugNumber()) ? issue.getLinkedBugNumber() : issue.getKey();
                if (idToUse != null && !idToUse.equals(issue.getKey())) {
                    log.info("Using linked bug number {} for issue {} as useLinkedBugzillaNumber is set to true", idToUse, issue.getKey());
                }
                bugNumbers = appendCsvValue(bugNumbers, idToUse);
            }
        }
    }

    public IssueInfo getIssueForBugNumber(String bugNumber) {
        for (IssueInfo issue : issues) {
            if (issue.getKey().equals(bugNumber)) {
                return issue;
            }
        }
        return null;
    }

    public List<String> bugNumbersAsList() {
        return StringUtils.splitAndTrim(bugNumbers, ",");
    }

    public List<JobBuild> jobBuildsMatchingUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return Collections.emptyList();
        }
        List<JobBuild> builds = new ArrayList<>();
        for (JobBuild buildToCheck : jobBuilds) {
            if (buildToCheck.containsUrl(url)) {
                builds.add(buildToCheck);
            }
        }
        return builds;
    }

    public boolean allJobBuildsMatchingUrlAreComplete(String url) {
        List<JobBuild> builds = jobBuildsMatchingUrl(url);
        for (JobBuild buildToCheck : builds) {
            if (buildToCheck.status == BuildStatus.BUILDING) {
                return false;
            }
        }
        return true;
    }

    public void updateTargetGroupsIfNeeded(String[] targetGroupsArray) {
        if (StringUtils.isNotEmpty(this.targetGroups)) { // added from draft
            return;
        }

        Set<String> targetGroups = new HashSet<>();
        for (Link group : reviewRequest.targetGroups) {
            log.debug("Adding review board group {}", group.getTitle());
            targetGroups.add(group.getTitle());
        }

        // only add target groups array if review is not public
        if (!reviewRequest.isPublic && targetGroupsArray != null) {
            targetGroups.addAll(Arrays.asList(targetGroupsArray));
        }

        this.targetGroups = StringUtils.join(targetGroups);
    }

    public void addExtraTargetGroupsIfNeeded() {
        if (extraTargetGroupsToAdd.isEmpty()) {
            return;
        }
        Set<String> existingTargetGroups = new TreeSet<>();
        if (targetGroups != null) {
            existingTargetGroups.addAll(Arrays.asList(targetGroups.trim().split(",")));
        }
        existingTargetGroups.addAll(extraTargetGroupsToAdd);
        targetGroups = StringUtils.join(existingTargetGroups);
    }

    public void updateTestingDoneWithJobBuild(Job job, JobBuild expectedNewBuild) {
        JobBuild existingBuild = getMatchingJobBuild(job);
        if (existingBuild == null ) {
            log.debug("Appending {} to testing done", expectedNewBuild.url);
            jobBuilds.add(expectedNewBuild);
        } else {
            log.debug("Replacing existing build url {} in testing done ", existingBuild.url);
            log.debug("New build url {}", expectedNewBuild.url);
            existingBuild.url = expectedNewBuild.url;
            existingBuild.status = expectedNewBuild.status;
        }
    }

    public boolean hasReviewers() {
        return isNotEmpty(reviewedBy);
    }

    /**
     * @return Whether the review request draft contains any actual data.
     */
    public boolean hasData() {
        boolean hasData;
        hasData = isNotEmpty(summary);
        hasData = isNotEmpty(description) || hasData;
        hasData = isNotEmpty(testingDone) || hasData;
        hasData = !jobBuilds.isEmpty() || hasData;
        hasData = isNotEmpty(bugNumbers) || hasData;
        hasData = hasReviewers() || hasData;
        hasData = hasReviewNumber() || hasData;
        return hasData;
    }

    public String toText(CommitConfig commitConfig) {
        return toText(commitConfig, true);
    }

    public String toText(CommitConfig commitConfig, boolean includeJobResults) {
        StringBuilder builder = new StringBuilder();
        builder.append(summary).append("\n\n");

        if (isNotEmpty(description)) {
            builder.append(description).append("\n");
        }
        String testingDoneSection = fullTestingDoneSection(includeJobResults);
        if (isNotEmpty(testingDoneSection)) {
            builder.append("\n").append(commitConfig.getTestingDoneLabel()).append(testingDoneSection);
        }
        if (isNotEmpty(bugNumbers)) {
            builder.append("\n").append(commitConfig.getBugNumberLabel()).append(bugNumbers);
        }
        if (isNotEmpty(reviewedBy)) {
            builder.append("\n").append(commitConfig.getReviewedByLabel()).append(reviewedBy);
        }
        if (hasReviewNumber()) {
            builder.append("\n").append(commitConfig.getReviewUrlLabel())
                    .append(addRelativePaths(commitConfig.getReviewboardUrl(), "r", id));
        } else if (isNotEmpty(id)) {
            builder.append("\n").append(commitConfig.getReviewUrlLabel()).append(id);
        }
        if (mergeToValues.length == 0 && commitConfig.getMergeToValues() != null) {
            mergeToValues = commitConfig.getMergeToValues();
        }
        for (String mergeToValue : mergeToValues) {
            builder.append("\n").append(commitConfig.getMergeToLabel()).append(mergeToValue);
        }
        if (isNotEmpty(approvedBy)) {
            builder.append("\n").append(commitConfig.getApprovedByLabel()).append(approvedBy);
        }
        if (isNotEmpty(pipeline)) {
            builder.append("\n").append(commitConfig.getPipelineLabel()).append(pipeline);
        }
        return builder.toString();
    }

    private String[] parseRepeatingSingleLineFromText(String text, String pattern, String description) {
        pattern = "^\\s*" + pattern; // ensure the first non whitespace on a line matches the pattern
        Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(text);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        if (matches.isEmpty()) {
            dynamicLog.log(DEBUG, "{} not found in commit", description);
            log.trace("Using pattern {} against text\n[{}]", pattern, text);
            return new String[0];
        }
        return matches.toArray(new String[0]);
    }

    private int parseValueFromText(String text, String pattern, String description) {
        String valueAsString = parseSingleLineFromText(text, pattern, description);
        return isNotEmpty(valueAsString) ? Integer.parseInt(valueAsString) : 0;
    }

    private String parseSingleLineFromText(String text, String pattern, String description) {
        return parseSingleLineFromText(text, pattern, description, DEBUG);
    }

    private String parseSingleLineFromText(String text, String pattern, String description, LogLevel logLevel) {
        pattern = "^\\s*" + pattern; // ensure the first non whitespace on a line matches the pattern
        return parseStringFromText(text, pattern, Pattern.MULTILINE, description, logLevel);
    }

    private String parseMultilineFromText(String text, String pattern, String description) {
        return parseStringFromText(text, pattern, Pattern.DOTALL, description);
    }

    private String parseStringFromText(String text, String pattern, int patternFlags, String description) {
        return parseStringFromText(text, pattern, patternFlags, description, DEBUG);
    }

    private String parseStringFromText(String text, String pattern, int patternFlags, String description, LogLevel logLevel) {
        Matcher matcher = Pattern.compile(pattern, patternFlags).matcher(text);
        if (!matcher.find()) {
            dynamicLog.log(logLevel, "{} not found in commit", description);
            log.trace("Using pattern {} against text\n[{}]", pattern, text);
            return "";
        }
        // get first non null group match
        for (int i = 1; i <= matcher.groupCount(); i ++) {
            String groupValue = matcher.group(i);
            if (groupValue != null) {
                return groupValue.trim();
            }
        }
        throw new RuntimeException("No non null group value for text [" + text + "] with pattern [" + pattern + "]");
    }

    public boolean isCommitSmallerThan(int maxFileCount, int maxLineCount) {
        return filesChanged <= maxFileCount || totalLineChanges() <= maxLineCount;
    }

    public boolean matchesAuthor(String authorEmails) {
        if (authorEmails == null) {
            return false;
        }
        return Arrays.stream(authorEmails.split(",")).map(String::trim)
                .anyMatch(authorEmail -> authorEmail.equals(this.authorEmail));
    }

    public String summaryInfo(int maxSummaryLength) {
        String truncatedSummary = truncateStringIfNeeded(summary, maxSummaryLength);
        String dateText = commitDate != null ? " " + new SimpleDateFormat("dd-MM-yyyy").format(commitDate) + "," : "";
        return String.format("%s:%s %s, %s", truncatedSummary, dateText,
                pluralize(filesChanged, "file"), pluralize(totalLineChanges(), "line change"));
    }

    public void setGitlabMergeRequest(MergeRequest mergeRequest) {
        this.gitlabMergeRequest = mergeRequest;
        this.requestUrl = mergeRequest.webUrl;
    }

    public MergeRequest getGitlabMergeRequest() {
        return gitlabMergeRequest;
    }

    public boolean hasMergeOrPullRequest() {
        return StringUtils.isNotBlank(requestUrl);
    }

    public PullRequest getGithubPullRequest() {
        return githubPullRequest;
    }

    public void setGithubPullRequest(PullRequest githubPullRequest) {
        this.githubPullRequest = githubPullRequest;
        this.requestUrl = githubPullRequest.url;
    }

    public Integer mergeRequestId() {
        return hasMergeOrPullRequest() ? Integer.parseInt(StringUtils.substringAfterLast(requestUrl, "/")) : null;
    }

    private int totalLineChanges() {
        return lineInsertions + lineDeletions;
    }
}

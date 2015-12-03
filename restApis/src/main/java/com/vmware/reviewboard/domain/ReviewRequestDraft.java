package com.vmware.reviewboard.domain;

import com.vmware.IssueInfo;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.JobBuildResult;
import com.vmware.jira.domain.Issue;
import com.vmware.utils.CommitConfiguration;
import com.vmware.utils.StringUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.utils.StringUtils.appendCsvValue;
import static com.vmware.utils.StringUtils.isNotBlank;
import static com.vmware.rest.UrlUtils.addTrailingSlash;

public class ReviewRequestDraft extends BaseEntity{
    @Expose(serialize = false, deserialize = false)
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Expose(serialize = false)
    public Integer id;
    @Expose(serialize = false, deserialize = false)
    public ReviewRequest reviewRequest;
    public String summary = "";
    public String description = "";
    @SerializedName("testing_done")
    public String testingDone = "";
    @Expose(deserialize = false)
    @SerializedName("bugs_closed")
    public String bugNumbers = "";
    @Expose(deserialize = false)
    @SerializedName("target_people")
    public String reviewedBy = "";
    @Expose(serialize = false, deserialize = false)
    public String shipItReviewers = "";
    @Expose(deserialize = false)
    public String target_groups = "";
    @Expose(serialize = false, deserialize = false)
    public List<JobBuild> jobBuilds = new ArrayList<>();
    @Expose(serialize = false, deserialize = false)
    public List<IssueInfo> issues = new ArrayList<>();
    public String branch = "";

    /**
     * Boolean object as review board 1.7 treats any value for isPublic as true.
     * To keep review request private, send null for isPublic
     */
    @SerializedName("public")
    public Boolean isPublic;

    @Expose(serialize = false, deserialize = false)
    public boolean jenkinsJobsAreSuccessful;

    @Expose(serialize = false, deserialize = false)
    public boolean hasFileChanges;

    @Expose(serialize = false, deserialize = false)
    public List<IssueInfo> openIssues = null;

    @Expose(serialize = false, deserialize = false)
    public boolean isPreloadingJiraIssues;

    @Expose(serialize = false, deserialize = false)
    public boolean isPreloadingBugzillaBugs;

    public ReviewRequestDraft() {}

    public ReviewRequestDraft(final String summary, final String description, final String testingDone,
                              final String bugNumbers, final String reviewedBy, String target_groups, String branch) {
        this.summary = summary;
        this.description = description;
        this.testingDone = testingDone;
        this.bugNumbers = bugNumbers;
        this.reviewedBy = reviewedBy;
        this.target_groups = target_groups;
        this.branch = branch;
    }

    public static ReviewRequestDraft anEmptyDraftForPublishingAReview() {
        ReviewRequestDraft draft = new ReviewRequestDraft(null,null,null,null,null,null,null);
        draft.isPublic = true;
        return draft;
    }

    public void fillValuesFromCommitText(String commitText, CommitConfiguration commitConfiguration) {
        String jenkinsUrl = commitConfiguration.getJenkinsUrl();
        String description = parseDescription(commitText, commitConfiguration.generateDescriptionPattern());
        String summary = commitText.substring(0, commitText.indexOf("\n"));
        description = description.length() < summary.length() + 1 ? "" : description.substring(summary.length() + 1);
        if (description.length() > 0 && description.charAt(0) == '\n') {
            description = description.substring(1);
        }
        String testingDoneSection = parseTestingDone(commitText, commitConfiguration.generateTestingDonePattern());

        this.id = parseReviewNumber(commitText);
        this.description = description;
        this.summary = summary;
        this.testingDone = stripJenkinsJobBuildsFromTestingDone(testingDoneSection, jenkinsUrl);
        this.jobBuilds = generateJobBuildsList(testingDoneSection, jenkinsUrl);
        this.bugNumbers = parseBugNumber(commitText, commitConfiguration.generateBugNumberPattern());
        this.reviewedBy = parseReviewedBy(commitText, commitConfiguration.generateReviewedByPattern());
    }

    public void addIssues(Issue[] issuesToAdd) {
        if (openIssues == null) {
            openIssues = new ArrayList<>();
        }
        for (Issue issueToAdd : issuesToAdd) {
            addOpenIssueInfo(issueToAdd);
        }
    }

    public void addBugs(List<Bug> bugsToAdd) {
        if (openIssues == null) {
            openIssues = new ArrayList<>();
        }
        for (Bug bugToAdd : bugsToAdd) {
            addOpenIssueInfo(bugToAdd);
        }
    }

    public void addOpenIssueInfo(IssueInfo issueInfo) {
        if (!openIssues.contains(issueInfo)) {
            openIssues.add(issueInfo);
        }
    }

    public boolean hasReviewNumber() {
        return id != null && id != 0;
    }

    public boolean hasBugNumber(String noBugNumberLabel) {
        return StringUtils.isNotBlank(bugNumbers) && !bugNumbers.equals(noBugNumberLabel);
    }

    public boolean isTrivialCommit(String trivialReviewerLabel) {
        return reviewedBy.equals(trivialReviewerLabel);
    }

    public String fullTestingDoneSection() {
        return fullTestingDoneSection(true);
    }

    public String fullTestingDoneSectionWithoutJobResults() {
        return fullTestingDoneSection(false);
    }

    private String fullTestingDoneSection(boolean includeResults) {
        String sectionText = this.testingDone;
        for (JobBuild build : jobBuilds) {
            sectionText += "\n" + build.url;
            if (includeResults) {
                sectionText += " " + build.result.name();
            }
        }
        return sectionText;
    }

    public JobBuild getMatchingJobBuild(String baseUrl) {
        for (JobBuild build : jobBuilds) {
            if (build.url.startsWith(baseUrl)) {
                return build;
            }
        }
        return null;
    }

    private String stripJenkinsJobBuildsFromTestingDone(String testingDone, String jenkinsUrl) {
        String patternToSearchFor = jenkinsUrl + "/job/[\\w-]+/\\d+/*\\s+" +
                JobBuildResult.generateResultPattern();
        return testingDone.replaceAll(patternToSearchFor, "").trim();
    }

    private List<JobBuild> generateJobBuildsList(String text, String jenkinsUrl) {
        Matcher jobMatcher = Pattern.compile("(" + jenkinsUrl + "/job/[\\w-]+/\\d+/*)\\s+" +
                JobBuildResult.generateResultPattern(), Pattern.MULTILINE).matcher(text);
        List<JobBuild> jobBuilds = new ArrayList<JobBuild>();
        while (jobMatcher.find()) {
            jobBuilds.add(new JobBuild(jobMatcher.group(1), JobBuildResult.valueOf(jobMatcher.group(2))));
        }
        return jobBuilds;
    }

    public void setIssues(List<IssueInfo> issues, String noBugNumberLabel) {
        this.issues.clear();
        this.issues.addAll(issues);

        this.bugNumbers = "";
        for (IssueInfo issue : issues) {
            if (issue == Issue.noBugNumber) {
                bugNumbers = appendCsvValue(bugNumbers, noBugNumberLabel);
            } else {
                bugNumbers = appendCsvValue(bugNumbers, issue.getKey());
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

    public void setTargetGroups(String[] targetGroupsArray) {
        List<String> targetGroups;
        if (targetGroupsArray != null) {
            targetGroups = new ArrayList<String>(Arrays.asList(targetGroupsArray));
        } else {
            targetGroups = new ArrayList<String>();
        }

        for (Link group : reviewRequest.targetGroups) {
            if (!targetGroups.contains(group.getTitle())) {
                log.debug("Adding review board group {}", group.getTitle());
                targetGroups.add(group.getTitle());
            }
        }

        this.target_groups = StringUtils.join(targetGroups);
    }

    public boolean hasReviewers() {
        return StringUtils.isNotBlank(reviewedBy);
    }

    /**
     * @return Whether the review request draft contains any actual data.
     */
    public boolean hasData() {
        boolean hasData = false;
        hasData = StringUtils.isNotBlank(summary) || hasData;
        hasData = StringUtils.isNotBlank(description) || hasData;
        hasData = StringUtils.isNotBlank(testingDone) || hasData;
        hasData = !jobBuilds.isEmpty() || hasData;
        hasData = StringUtils.isNotBlank(bugNumbers) || hasData;
        hasData = hasReviewers() || hasData;
        hasData = hasReviewNumber() || hasData;
        return hasData;
    }

    public String toGitText(CommitConfiguration commitConfig) {
        StringBuilder builder = new StringBuilder();
        builder.append(summary).append("\n\n");

        if (StringUtils.isNotBlank(description)) {
            builder.append(description).append("\n\n");
        }

        if (isNotBlank(fullTestingDoneSection())) {
            builder.append(commitConfig.getTestingDoneLabel()).append(fullTestingDoneSection()).append("\n");
        }
        if (isNotBlank(bugNumbers)) {
            builder.append(commitConfig.getBugNumberLabel()).append(bugNumbers).append("\n");
        }
        if (isNotBlank(reviewedBy)) {
            builder.append(commitConfig.getReviewedByLabel()).append(reviewedBy);
        }
        if (hasReviewNumber()) {
            builder.append("\n").append(commitConfig.getReviewUrlLabel())
                    .append(addTrailingSlash(commitConfig.getReviewboardUrl())).append("r/").append(id);
        }
        return builder.toString();
    }

    private String parseDescription(String text, String pattern) {
        Matcher descriptionMatcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(text);
        if (!descriptionMatcher.find()) {
            log.warn("Description not found");
            return "";
        }
        return descriptionMatcher.group(1).trim();
    }

    private String parseBugNumber(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(text);
        if (!matcher.find()) {
            log.warn("Bug Number not found");
            return "";
        }
        return matcher.group(1).trim();
    }

    private String parseReviewedBy(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(text);
        if (!matcher.find()) {
            log.warn("No reviewers found in commit");
            return "";
        }
        return matcher.group(1).trim();
    }

    private String parseTestingDone(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(text);
        if (!matcher.find()) {
            log.warn("Testing Done section not found");
            return "";
        }
        return matcher.group(1).trim();
    }

    private Integer parseReviewNumber(String text) {
        Matcher reviewMatcher = Pattern.compile("com/r/(\\d+)/*\\s*$", Pattern.MULTILINE).matcher(text);
        if (!reviewMatcher.find()) {
            return null;
        }
        return Integer.valueOf(reviewMatcher.group(1));
    }

}

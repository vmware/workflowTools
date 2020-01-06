package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;

import java.util.Arrays;

/**
 * Configuration information for a commit. Used as a transport class for a review request draft;
 */
public class CommitConfig {

    @ConfigurableProperty(commandLine = "-reviewboardUrl,--reviewboard-url", help = "Url for review board server", gitConfigProperty = "reviewboard.url")
    public String reviewboardUrl;

    @ConfigurableProperty(commandLine = "-jenkinsUrl,--jenkins-url", help = "Url for jenkins server")
    public String jenkinsUrl;

    @ConfigurableProperty(commandLine = "-buildwebUrl,--buildweb-url", help = "Url for buildweb server")
    public String buildwebUrl;

    @ConfigurableProperty(commandLine = "-ms,--max-summary", help = "Sets max line length for the one line summary")
    public int maxSummaryLength;

    @ConfigurableProperty(commandLine = "-md,--max-description", help = "Sets max line length for all other lines in the commit")
    public int maxDescriptionLength;

    @ConfigurableProperty(help = "Label for no review number, only relevant if review url is always included")
    public String noReviewNumberLabel;

    @ConfigurableProperty(help = "Label for no bug number")
    public String noBugNumberLabel;

    @ConfigurableProperty(help = "Label for trivial reviewer")
    public String trivialReviewerLabel;

    @ConfigurableProperty(help = "Label for merge to value")
    public String mergeToLabel;

    @ConfigurableProperty(help = "Label for approved by value")
    public String approvedByLabel;

    @ConfigurableProperty(help = "Label for testing done section")
    public String testingDoneLabel;

    @ConfigurableProperty(help = "Label for bug number")
    public String bugNumberLabel;

    @ConfigurableProperty(help = "Label for reviewed by")
    public String reviewedByLabel;

    @ConfigurableProperty(help = "Label for review url")
    public String reviewUrlLabel;

    @ConfigurableProperty(commandLine = "--merge-to", help = "Comma separate values for merge to property")
    public String[] mergeToValues;

    @ConfigurableProperty(commandLine = "--merge-to-default", help = "Default value for merge to property if none set")
    public String mergeToDefault;

    @ConfigurableProperty(help = "Default value to set for topic if none entered")
    public String defaultTopic;

    @ConfigurableProperty(commandLine = "-da,--default-approver", help = "Default user to use for approving")
    public String defaultApprover;

    @ConfigurableProperty(help = "Template values for topic, press up to cycle through values when entering topic")
    public String[] topicTemplates;

    @ConfigurableProperty(help = "Template values for testing done, press up to cycle through values when entering testing done")
    public String[] testingDoneTemplates;

    @ConfigurableProperty(commandLine = "--set-empty-only", help = "Set values for empty properties only. Ignore properties that already have values")
    public boolean setEmptyPropertiesOnly;

    @ConfigurableProperty(commandLine = "--disable-merge-to", help = "Disable merge to")
    public boolean disableMergeTo;

    public CommitConfig() {}

    public CommitConfig(String reviewboardUrl, String buildwebUrl, String jenkinsUrl, String testingDoneLabel, String bugNumberLabel,
                               String reviewedByLabel, String reviewUrlLabel, String mergeToLabel, String[] mergeToValues,
                               String approvedByLabel) {
        this.reviewboardUrl = reviewboardUrl;
        this.jenkinsUrl = jenkinsUrl;
        this.testingDoneLabel = padLabel(testingDoneLabel);
        this.bugNumberLabel = padLabel(bugNumberLabel);
        this.reviewedByLabel = padLabel(reviewedByLabel);
        this.reviewUrlLabel = padLabel(reviewUrlLabel);
        this.mergeToLabel = padLabel(mergeToLabel);
        this.buildwebUrl = buildwebUrl;
        this.approvedByLabel = padLabel(approvedByLabel);
        this.mergeToValues = mergeToValues;

    }

    public String generateDescriptionPattern() {
        StringBuilder builder = new StringBuilder();
        builder.append("(.+?)(");
        appendLabelToPattern(builder, reviewedByLabel);
        appendLabelToPattern(builder, testingDoneLabel);
        appendLabelToPattern(builder, bugNumberLabel);
        appendLabelToPattern(builder, reviewUrlLabel);
        appendLabelToPattern(builder, mergeToLabel);
        appendLabelToPattern(builder, approvedByLabel);
        appendLabelToPattern(builder, "\\s+\\d+\\s+files*\\s+changed");
        builder.append("($))");
        return builder.toString();
    }

    public String generateTestingDonePattern() {
        StringBuilder builder = new StringBuilder();
        builder.append(testingDoneLabel.trim()).append("\\s*(.*?)(");
        appendLabelToPattern(builder, reviewedByLabel);
        appendLabelToPattern(builder, bugNumberLabel);
        appendLabelToPattern(builder, reviewUrlLabel);
        appendLabelToPattern(builder, mergeToLabel);
        appendLabelToPattern(builder, approvedByLabel);
        appendLabelToPattern(builder, "\\s+\\d+\\s+ file\\w*\\s+changed");
        builder.append("($))");
        return builder.toString();
    }

    public String generateReviewUrlPattern() {
        return reviewUrlLabel.trim() + "\\s*(?:(?:\\S+/r/(\\d+)/*)|(\\w+))\\s*$";
    }

    public String generateReviewedByPattern() {
        return reviewedByLabel.trim() + "\\s*([\\w,\\s]+)$";
    }

    public String generateBugNumberPattern() {
        return bugNumberLabel.trim() + "([,\\w\\d\\s-]+)$";
    }

    public String generateMergeToPattern() {
        return mergeToLabel.trim() + "\\s*(.+)$";
    }

    public String generateApprovedByPattern() {
        return approvedByLabel.trim() + "\\s*(.+)$";
    }

    public String generateBuildWebIdPattern() {
        return "http.+?/(\\w\\w/\\d+)/*";
    }

    public String generateBuildUrlsPattern() {
        return "((?:" + StringUtils.join(Arrays.asList(buildwebUrl, jenkinsUrl), "|") + ")\\S+)";
    }

    public String getReviewboardUrl() {
        return reviewboardUrl;
    }

    public String getTestingDoneLabel() {
        return padLabel(testingDoneLabel);
    }

    public String getBugNumberLabel() {
        return padLabel(bugNumberLabel);
    }

    public String getReviewedByLabel() {
        return padLabel(reviewedByLabel);
    }

    public String getReviewUrlLabel() {
        return padLabel(reviewUrlLabel);
    }

    public String getMergeToLabel() {
        return padLabel(mergeToLabel);
    }

    public String[] getMergeToValues() {
        return mergeToValues;
    }

    public String getApprovedByLabel() {
        return padLabel(approvedByLabel);
    }

    private void appendLabelToPattern(StringBuilder builder, String label) {
        builder.append("(").append(label.trim()).append(")|");
    }


    private String padLabel(String label) {
        if (label == null) {
            return null;
        }
        return label.endsWith(" ") ? label : label + " ";
    }
}

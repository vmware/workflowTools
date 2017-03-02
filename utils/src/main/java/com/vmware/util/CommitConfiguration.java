package com.vmware.util;

import java.util.Set;

/**
 * Configuration information for a commit. Used as a transport class for a review request draft;
 */
public class CommitConfiguration {

    private final String reviewboardUrl;
    private final String buildwebUrl;
    private final String approvedByLabel;
    private Set<String> jenkinsJobNames;
    private String testingDoneLabel;

    private String bugNumberLabel;

    private String reviewedByLabel;

    private String reviewUrlLabel;

    private String mergeToLabel;

    private String[] mergeToValues;

    public CommitConfiguration(String reviewboardUrl, String buildwebUrl, Set<String> jenkinsJobNames, String testingDoneLabel, String bugNumberLabel,
                               String reviewedByLabel, String reviewUrlLabel, String mergeToLabel, String[] mergeToValues,
                               String approvedByLabel) {
        this.reviewboardUrl = reviewboardUrl;
        this.jenkinsJobNames = jenkinsJobNames;
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

    public String generateBuildWebNumberPattern() {
        return "http.+?/sb/(\\d+)/*";
    }

    public String buildWebUrl() {
        return buildwebUrl + "/sb";
    }

    public Set<String> getJenkinsJobNames() {
        return jenkinsJobNames;
    }

    public String getReviewboardUrl() {
        return reviewboardUrl;
    }

    public String getTestingDoneLabel() {
        return testingDoneLabel;
    }

    public String getBugNumberLabel() {
        return bugNumberLabel;
    }

    public String getReviewedByLabel() {
        return reviewedByLabel;
    }

    public String getReviewUrlLabel() {
        return reviewUrlLabel;
    }

    public String getMergeToLabel() {
        return mergeToLabel;
    }

    public String[] getMergeToValues() {
        return mergeToValues;
    }

    public String getApprovedByLabel() {
        return approvedByLabel;
    }

    private void appendLabelToPattern(StringBuilder builder, String label) {
        builder.append("(").append(label.trim()).append(")|");
    }


    private String padLabel(String label) {
        return label.endsWith(" ") ? label : label + " ";
    }
}

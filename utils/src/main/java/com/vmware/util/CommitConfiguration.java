package com.vmware.util;

/**
 * Configuration information for a commit. Used as a transport class for a review request draft;
 */
public class CommitConfiguration {

    private final String jenkinsUrl;
    private String reviewboardUrl;

    private String testingDoneLabel;

    private String bugNumberLabel;

    private String reviewedByLabel;

    private String reviewUrlLabel;

    private String noBugNumberLabel;

    private String trivialReviewerLabel;

    public CommitConfiguration(String reviewboardUrl, String jenkinsUrl , String testingDoneLabel, String bugNumberLabel,
                               String reviewedByLabel, String reviewUrlLabel, String noBugNumberLabel, String trivialReviewerLabel) {
        this.reviewboardUrl = reviewboardUrl;
        this.jenkinsUrl = jenkinsUrl;
        this.testingDoneLabel = padLabel(testingDoneLabel);
        this.bugNumberLabel = padLabel(bugNumberLabel);
        this.reviewedByLabel = padLabel(reviewedByLabel);
        this.reviewUrlLabel = padLabel(reviewUrlLabel);
        this.noBugNumberLabel = noBugNumberLabel;
        this.trivialReviewerLabel = trivialReviewerLabel;
    }

    public String generateDescriptionPattern() {
        StringBuilder builder = new StringBuilder();
        builder.append("(.+?)(");
        appendLabelToPattern(builder, reviewedByLabel);
        appendLabelToPattern(builder, testingDoneLabel);
        appendLabelToPattern(builder, bugNumberLabel);
        appendLabelToPattern(builder, reviewUrlLabel);
        builder.append("($))");
        return builder.toString();
    }

    public String generateTestingDonePattern() {
        StringBuilder builder = new StringBuilder();
        builder.append(testingDoneLabel.trim()).append("\\s*(.*?)(");
        appendLabelToPattern(builder, reviewedByLabel);
        appendLabelToPattern(builder, bugNumberLabel);
        appendLabelToPattern(builder, reviewUrlLabel);
        builder.append("($))");
        return builder.toString();
    }

    public String generateReviewedByPattern() {
        return reviewedByLabel.trim() + "\\s*([\\w,\\s]+)$";
    }

    public String generateBugNumberPattern() {
        return bugNumberLabel.trim() + "([,\\w\\d\\s-]+)$";
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
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

    public String getNoBugNumberLabel() {
        return noBugNumberLabel;
    }

    public String getTrivialReviewerLabel() {
        return trivialReviewerLabel;
    }

    private void appendLabelToPattern(StringBuilder builder, String label) {
        builder.append("(").append(label.trim()).append(")|");
    }


    private String padLabel(String label) {
        return label.endsWith(" ") ? label : label + " ";
    }
}

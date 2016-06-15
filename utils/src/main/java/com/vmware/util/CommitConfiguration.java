package com.vmware.util;

/**
 * Configuration information for a commit. Used as a transport class for a review request draft;
 */
public class CommitConfiguration {

    private final String jenkinsUrl;
    private final String reviewboardUrl;
    private final String buildwebApiUrl;
    private String testingDoneLabel;

    private String bugNumberLabel;

    private String reviewedByLabel;

    private String reviewUrlLabel;

    public CommitConfiguration(String reviewboardUrl, String jenkinsUrl, String buildwebApiUrl, String testingDoneLabel, String bugNumberLabel,
                               String reviewedByLabel, String reviewUrlLabel) {
        this.reviewboardUrl = reviewboardUrl;
        this.jenkinsUrl = jenkinsUrl;
        this.testingDoneLabel = padLabel(testingDoneLabel);
        this.bugNumberLabel = padLabel(bugNumberLabel);
        this.reviewedByLabel = padLabel(reviewedByLabel);
        this.reviewUrlLabel = padLabel(reviewUrlLabel);
        this.buildwebApiUrl = buildwebApiUrl;
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

    public String generateReviewUrlPattern() {
        return reviewUrlLabel.trim() + "\\s*.+?/r/(\\d+)/*\\s*$";
    }

    public String generateReviewedByPattern() {
        return reviewedByLabel.trim() + "\\s*([\\w,\\s]+)$";
    }

    public String generateBugNumberPattern() {
        return bugNumberLabel.trim() + "([,\\w\\d\\s-]+)$";
    }

    public String generateJenkinsUrlPattern() {
        return "(" + jenkinsUrl + "/job/.+?/\\d+/*)";
    }

    public String generateBuildWebNumberPattern() {
        return "http.+?/sb/(\\d+)/*";
    }

    public String generateFullBuildwebApiUrlPattern() {
        return "(" + sandboxBuildwebUrl() + "/\\d+/*)";
    }

    public String sandboxBuildwebUrl() {
        return buildwebApiUrl + "/sb/build";
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

    private void appendLabelToPattern(StringBuilder builder, String label) {
        builder.append("(").append(label.trim()).append(")|");
    }


    private String padLabel(String label) {
        return label.endsWith(" ") ? label : label + " ";
    }
}

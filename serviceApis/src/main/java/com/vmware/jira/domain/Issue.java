package com.vmware.jira.domain;

import com.vmware.IssueInfo;
import com.vmware.config.jira.IssueTypeDefinition;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import com.google.gson.annotations.Expose;

import java.util.List;

public class Issue implements IssueInfo {

    public static Issue noBugNumber = Issue.noBugNumber();

    @Expose(serialize = false)
    public int id;

    @Expose(serialize = false, deserialize = false)
    public boolean isNotFound;

    @Expose(serialize = false, deserialize = false)
    public boolean hasNoBugNumber;

    @Expose(serialize = false)
    public String self;

    private String key;
    public IssueFields fields;

    public Issue() {
    }

    public Issue(String key) {
        this.key = key;
        this.fields = new IssueFields();
    }

    public Issue(IssueTypeDefinition issueType, String project, String component, String summary,
                 String description, String acceptanceCriteria) {
        IssueFields fields = new IssueFields();
        fields.project = new Project(project);
        fields.components = new Component[] {new Component(component)};
        fields.issuetype = new IssueType(issueType);
        fields.summary = summary;
        fields.description = description;
        fields.acceptanceCriteria = acceptanceCriteria;
        this.fields = fields;
    }

    public boolean hasFieldValue(FilterableIssueField fieldType, String value) {
        List<String> values = fields.valuesForFilterableField(fieldType);
        return values.contains(value);
    }

    private static Issue noBugNumber() {
        Issue response = new Issue();
        response.hasNoBugNumber = true;
        return response;
    }

    public static Issue aNotFoundIssue(String key) {
        Issue response = new Issue();
        response.isNotFound = true;
        response.key = key;
        return response;
    }

    public boolean isReal() {
        return !isNotFound && !hasNoBugNumber;
    }

    public boolean isFromJira() {
        return StringUtils.isNotEmpty(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof String) {
            String key = (String) o;
            return key.equals(this.key);
        }

        if (o == null || getClass() != o.getClass()) return false;

        Issue issue = (Issue) o;

        if (key != null ? !key.equals(issue.key) : issue.key != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "key='" + key + '\'' +
                ", fields=" + fields +
                '}';
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getWebUrl() {
        if (self == null) {
            return null;
        }
        String jiraUrl = MatcherUtils.singleMatch(self, "(.+?/)rest/api");
        return jiraUrl == null ? null : jiraUrl + "browse/" + key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getSummary() {
        return fields.summary;
    }

    @Override
    public String getDescription() {
        return fields.description;
    }

    public IssueResolutionDefinition getResolution() {
        return fields.resolution != null ? fields.resolution.definition : null;
    }

    public Integer matchingBugzillaNumber(String bugzillaUrl) {
        String bugzillaNumber = MatcherUtils.singleMatch(fields.description, bugzillaUrl + "/*show_bug\\.cgi\\?id=(\\d+)");
        return bugzillaNumber != null ? Integer.parseInt(bugzillaNumber) : null;
    }


    @Override
    public boolean isNotFound() {
        return isNotFound;
    }

    public IssueStatusDefinition getStatus() {
        return fields.status != null ? fields.status.def : IssueStatusDefinition.UnknownValue;
    }
}

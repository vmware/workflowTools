package com.vmware.jira.domain;

import com.vmware.http.json.RuntimeFieldName;
import com.vmware.util.StringUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.exception.FatalException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IssueFields {
    public Project project;

    public Component[] components;

    public String summary;

    public String description;

    @Expose(serialize = false)
    public List<String> labels;

    @Expose(serialize = false)
    public List<FixVersion> fixVersions;

    public JiraUser assignee;

    @Expose(serialize = false)
    @SerializedName("timeoriginalestimate")
    public long originalEstimateInSeconds;

    @Expose(serialize = false)
    public IssueStatus status;

    public IssueType issuetype;

    @Expose(deserialize = false)
    public IssueResolution resolution;

    @Expose(deserialize = false)
    public IssueTimeTracking timetracking;

    @RuntimeFieldName("storyPointsFieldName")
    public Number storyPoints;

    @RuntimeFieldName("acceptanceCriteriaFieldName")
    public String acceptanceCriteria;

    @RuntimeFieldName("parentEpicFieldName")
    public String parentEpic;

    public boolean storyPointsEqual(Number value) {
        if (storyPoints == null) {
            return value == null;
        }
        return value != null && value.equals(storyPoints.intValue());
    }

    @Override
    public String toString() {
        return "IssueFields{" +
                "summary='" + summary + '\'' +
                '}';
    }

    public String getComponentsText() {
        if (components == null) {
            return null;
        }
        String text = "";
        for (Component component : components) {
            StringUtils.appendCsvValue(text, component.name);
        }
        return text;
    }

    public List<String> valuesForFilterableField(FilterableIssueField field) {
        switch (field) {
            case label:
                return labels != null ? labels : Collections.emptyList();
            case fixByVersion:
                return fixVersions != null
                        ? fixVersions.stream().map(fixVersion -> fixVersion.name).collect(Collectors.toList()) : Collections.emptyList();
            case epic:
                return StringUtils.isNotEmpty(parentEpic) ? Collections.singletonList(parentEpic) : Collections.emptyList();
            default:
                throw new FatalException("No handling for filterable field " + field);
        }
    }
}

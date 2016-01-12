package com.vmware.jira.domain;

import com.vmware.util.StringUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class IssueFields {

    public static final String STORY_POINTS_FIELD = "customfield_10062";

    public static final String ACCEPTANCE_CRITERIA_FIELD = "customfield_10100";

    public Project project;

    public Component[] components;

    public String summary;

    public String description;

    @Expose(serialize = false)
    public String[] labels;

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

    @SerializedName(STORY_POINTS_FIELD)
    public Number storyPoints;

    @SerializedName(ACCEPTANCE_CRITERIA_FIELD)
    public String acceptanceCriteria;


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
}

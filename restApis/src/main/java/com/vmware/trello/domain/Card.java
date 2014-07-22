package com.vmware.trello.domain;

import com.google.gson.annotations.Expose;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueFields;
import com.vmware.utils.MatcherUtils;
import com.vmware.utils.StringUtils;
import com.vmware.utils.UrlUtils;

import java.util.regex.Pattern;

public class Card {
    public String idList;
    public String name;
    public String desc;

    @Expose(serialize = false)
    public String id;
    @Expose(serialize = false)
    public String idBoard;
    public CardLabel[] labels;
    @Expose(serialize = false)
    public String shortUrl;
    @Expose(serialize = false)
    public String url;

    private Card() {}

    public Card(Swimlane swimlane, String name) {
        this.idList = swimlane.id;
        this.name = name;
    }

    public Card(Swimlane swimlane, Issue issue, String jiraUrl) {
        this.idList = swimlane.id;

        IssueFields details = issue.fields;
        this.name = details.summary;

        this.desc = "";
        if (StringUtils.isNotBlank(details.acceptanceCriteria)) {
            this.desc += "**  Acceptance Criteria  **\n" + details.acceptanceCriteria + "\n\n";
        }

        this.desc += "**  Description  **\n" + details.description + "\n";

        String urlForIssue = UrlUtils.addTrailingSlash(jiraUrl) + "browse/" + issue.key;

        this.desc += urlForIssue;
    }

    public String getIssueKey() {
        return MatcherUtils.singleMatch(desc, "browse/([\\w-]+)");
    }

    public String getDescriptionWithoutJiraUrl(String jiraUrl) {
        String value = MatcherUtils.singleMatch(desc, "\\*\\*\\s*Description\\s*\\*\\*(.+)", Pattern.DOTALL);
        if (value == null) {
            // return all the description if there is no match
            return !desc.isEmpty() ? desc : "No description";
        }
        return value.endsWith(jiraUrl) ? value.substring(0, value.indexOf(jiraUrl)).trim() : value.trim();
    }

    public String getAcceptanceCriteria() {
        String value = MatcherUtils.singleMatch(desc,
                "\\*\\*\\s*Acceptance Criteria\\s*\\*\\*(.+)\\*\\*\\s*Description\\s*\\*\\*", Pattern.DOTALL);
        return value != null ? value.trim() : "to do";
    }
}

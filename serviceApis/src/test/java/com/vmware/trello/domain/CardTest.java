package com.vmware.trello.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vmware.jira.domain.Issue;

/**
 * Test Card object.
 */
public class CardTest {
    @Test
    public void canGetIssueKey() {
        String issueKey = "HW-34305";

        Card card = new Card(new Swimlane(new Board("board-name"), "swim-lane"), new Issue(issueKey), "jira-url");
        assertEquals(issueKey, card.getIssueKey());
    }
}

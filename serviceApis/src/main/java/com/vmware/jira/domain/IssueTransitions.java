package com.vmware.jira.domain;

import com.google.gson.annotations.Expose;

public class IssueTransitions {

    @Expose(serialize = false, deserialize = false)
    public String issueKey;

    public String expand;

    private IssueTransition[] transitions = new IssueTransition[0];

    public boolean canTransitionTo(IssueStatusDefinition toStatus) {
        for (IssueTransition transition : transitions) {
            if (transition.to.def == toStatus) {
                return true;
            }
        }
        return false;
    }

    public IssueTransition getTransitionForStatus(IssueStatusDefinition toStatus) {
        for (IssueTransition transition : transitions) {
            if (transition.to.def == toStatus) {
                transition.issueId = issueKey;
                return transition;
            }
        }
        throw new IllegalArgumentException("No transition available for status " + toStatus.name());
    }
}

/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.action.AbstractAction;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueTypeDefinition;
import com.vmware.jira.domain.ProjectIssues;
import com.vmware.rest.UrlUtils;

public abstract class AbstractBatchIssuesAction extends AbstractAction {

    protected ProjectIssues projectIssues;

    public AbstractBatchIssuesAction(WorkflowConfig config) {
        super(config);
    }

    public void setProjectIssues(ProjectIssues projectIssues) {
        this.projectIssues = projectIssues;
    }

}

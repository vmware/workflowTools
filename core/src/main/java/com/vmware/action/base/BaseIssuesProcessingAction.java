/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.ProjectIssues;

public abstract class BaseIssuesProcessingAction extends BaseAction {

    protected ProjectIssues projectIssues;

    public BaseIssuesProcessingAction(WorkflowConfig config) {
        super(config);
    }

    public void setProjectIssues(ProjectIssues projectIssues) {
        this.projectIssues = projectIssues;
    }

}

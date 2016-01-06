/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.action.AbstractAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.MultiActionData;

public abstract class AbstractBatchIssuesAction extends AbstractAction {

    protected MultiActionData multiActionData;

    public AbstractBatchIssuesAction(WorkflowConfig config) {
        super(config);
    }

    public void setMultiActionData(MultiActionData multiActionData) {
        this.multiActionData = multiActionData;
    }

}

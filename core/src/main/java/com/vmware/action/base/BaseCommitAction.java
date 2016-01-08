/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;

public abstract class BaseCommitAction extends BaseAction {

    protected ReviewRequestDraft draft;

    public BaseCommitAction(WorkflowConfig config) {
        super(config);
    }

    public void setDraft(ReviewRequestDraft draft) {
        this.draft = draft;
    }
}

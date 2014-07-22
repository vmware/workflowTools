/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.action.AbstractAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;

public abstract class AbstractCommitAction extends AbstractAction {

    protected ReviewRequestDraft draft;

    public AbstractCommitAction(WorkflowConfig config) {
        super(config);
    }

    public void setDraft(ReviewRequestDraft draft) {
        this.draft = draft;
    }
}

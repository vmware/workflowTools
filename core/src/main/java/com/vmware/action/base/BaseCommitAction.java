/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

public abstract class BaseCommitAction extends BaseAction {

    protected ReviewRequestDraft draft;

    public BaseCommitAction(WorkflowConfig config) {
        super(config);
    }

    public void setDraft(ReviewRequestDraft draft) {
        this.draft = draft;
    }

    protected String readLastChange() {
        if (git.workingDirectoryIsInGitRepo()) {
            return git.lastCommitText(true);
        } else if (StringUtils.isNotBlank(config.perforceClientName)) {
            return readPendingChangelistText();
        } else {
            log.warn("Not in git repo and config value perforceClientName is not set, can't read last change");
            return "";
        }
    }

    private String readPendingChangelistText() {
        String changelistId = serviceLocator.getPerforce().selectPendingChangelist();
        String changelistText = serviceLocator.getPerforce().readChangelist(changelistId);
        if (StringUtils.isBlank(changelistText) || !changelistText.contains("\n")) {
            throw new RuntimeException("No pending changelist exists for user " + config.username);
        }
        return changelistText;
    }
}

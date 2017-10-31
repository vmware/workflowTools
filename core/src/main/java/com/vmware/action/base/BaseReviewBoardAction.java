package com.vmware.action.base;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;

public abstract class BaseReviewBoardAction extends BaseAction {

    protected ReviewBoard reviewBoard;

    public BaseReviewBoardAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        reviewBoard = serviceLocator.getReviewBoard();
    }
}

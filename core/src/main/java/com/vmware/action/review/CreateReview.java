package com.vmware.action.review;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.util.StringUtils;

@ActionDescription("Always creates a new review, even if there is an existing review url in the commit.")
public class CreateReview extends BaseCommitAction {
    private ReviewBoard reviewBoard;

    public CreateReview(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(config.reviewBoardRepository)) {
            return "no reviewboard repository is configured, set a config value for reviewBoardRepository";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void preprocess() {
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(config.reviewBoardDateFormat);
    }

    @Override
    public void process() {
        if (!draft.hasReviewNumber()) {
            log.info("Creating new review against repository {}", config.reviewBoardRepository);
        } else {
            log.info("Creating new review against repository {} to replace review {} for this commit",
                    config.reviewBoardRepository, draft.id);
        }

        draft.reviewRequest = reviewBoard.createReviewRequest(config.reviewBoardRepository);
        Repository repository = reviewBoard.getRepository(draft.reviewRequest.getRepositoryLink());
        draft.repoType = repository.tool.toLowerCase();
        draft.id = String.valueOf(draft.reviewRequest.id);
        log.info("Created empty review {}", draft.id);
    }
}

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
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        super.failIfTrue(StringUtils.isEmpty(reviewBoardConfig.reviewBoardRepository),
                "no reviewboard repository is configured, set a config value for reviewBoardRepository");
    }

    @Override
    public void preprocess() {
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(reviewBoardConfig.reviewBoardDateFormat);
    }

    @Override
    public void process() {
        if (!draft.hasReviewNumber()) {
            log.info("Creating new review against repository {}", reviewBoardConfig.reviewBoardRepository);
        } else {
            log.info("Creating new review against repository {} to replace review {} for this commit",
                    reviewBoardConfig.reviewBoardRepository, draft.id);
        }

        draft.reviewRequest = reviewBoard.createReviewRequest(reviewBoardConfig.reviewBoardRepository);
        Repository repository = reviewBoard.getRepository(draft.reviewRequest.getRepositoryLink());
        draft.repoType = repository.getRepoType();
        draft.id = String.valueOf(draft.reviewRequest.id);
        log.info("Created empty review {}", draft.id);
    }
}

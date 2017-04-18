package com.vmware.action.review;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.RepoType;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.util.StringUtils;

@ActionDescription("Only creates a new review if there is not an existing review url in the commit.")
public class CreateReviewIfNeeded extends BaseCommitAction {
    private ReviewBoard reviewBoard;

    public CreateReviewIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.hasReviewNumber()) {
            return "commit already has review " + draft.id;
        }
        return super.cannotRunAction();
    }

    @Override
    public void asyncSetup() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void preprocess() {
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(config.reviewBoardDateFormat);
    }

    @Override
    public void process() {
        if (StringUtils.isBlank(config.reviewBoardRepository)) {
            throw new RuntimeException("no reviewboard repository is configured, set a config value for reviewBoardRepository");
        }
        log.info("Creating new review against repository {}", config.reviewBoardRepository);
        draft.reviewRequest = reviewBoard.createReviewRequest(config.reviewBoardRepository);
        Repository repository = reviewBoard.getRepository(draft.reviewRequest.getRepositoryLink());
        draft.repoType = repository.getRepoType();
        draft.id = String.valueOf(draft.reviewRequest.id);
        log.info("Created new review {}", draft.reviewRequest.id);
    }
}

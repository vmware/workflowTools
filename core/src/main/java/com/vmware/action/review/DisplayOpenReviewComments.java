package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewComment;
import com.vmware.reviewboard.domain.DiffCommentStatus;
import com.vmware.reviewboard.domain.UserReview;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ActionDescription("Displays open diff and general comments for review.")
public class DisplayOpenReviewComments extends BaseCommitUsingReviewBoardAction {
    public DisplayOpenReviewComments(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (!draft.reviewRequest.isPublic) {
            return;
        }
        UserReview[] reviews = reviewBoard.getReviewsForReviewRequest(draft.reviewRequest.getReviewsLink());
        for (UserReview review : reviews) {
            String boatIt = review.ship_it ? "Boated" : "No Boat";
            Padder reviewPadder = new Padder(review.getReviewUsername() + " - " + boatIt);
            reviewPadder.infoTitle();
            if (StringUtils.isNotBlank(review.body_top)) {
                log.info(review.body_top);
            }
            ReviewComment[] generalComments = review.ship_it ?
                    new ReviewComment[0] : reviewBoard.getGeneralCommentsForReview(review.getGeneralCommentsLink());
            if (StringUtils.isNotBlank(review.body_top) && generalComments.length > 0) {
                log.info("");
            }
            IntStream.range(0, generalComments.length).forEach(index -> {
                if (index > 0) {
                    log.info("");
                }
                log.info(generalComments[index].text);
            });

            ReviewComment[] comments = reviewBoard.getDiffCommentsForReview(review.getDiffCommentsLink());
            List<ReviewComment> openComments = Arrays.stream(comments).filter(comment -> comment.status == DiffCommentStatus.open).collect(Collectors.toList());
            if (openComments.isEmpty()) {
                if (StringUtils.isEmpty(review.body_top) && generalComments.length == 0) {
                    log.info("No comments or open issues");
                }
                reviewPadder.infoTitle();
                continue;
            }

            if (StringUtils.isNotBlank(review.body_top) || generalComments.length > 0) {
                log.info("");
            }

            IntStream.range(0, openComments.size())
                    .forEach(commentIndex -> {
                        if (commentIndex > 0) {
                            log.info("");
                        }
                        ReviewComment comment = openComments.get(commentIndex);
                        log.info("{}\n{}", comment.fileNameAndLineNumber(), comment.text);
                    });
            reviewPadder.infoTitle();
        }
    }
}

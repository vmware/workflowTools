package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestNote;
import com.vmware.reviewboard.domain.ReviewComment;
import com.vmware.reviewboard.domain.DiffCommentStatus;
import com.vmware.reviewboard.domain.UserReview;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ActionDescription("Displays open diff and general comments for review.")
public class DisplayOpenDiffNotes extends BaseCommitWithMergeRequestAction {
    public DisplayOpenDiffNotes(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        Set<MergeRequestNote> diffNotes = gitlab.getOpenMergeRequestNotes(mergeRequest.projectId, mergeRequest.iid);
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd hh:mm:ss");
        for (MergeRequestNote diffNote : diffNotes) {
            Padder diffPadder = new Padder(diffNote.author.name + " " + formatter.format(diffNote.createdAt));
            diffPadder.infoTitle();
            log.info(diffNote.body);
            diffPadder.infoTitle();
        }
    }
}

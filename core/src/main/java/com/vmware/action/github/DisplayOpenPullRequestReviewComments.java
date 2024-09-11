package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReviewComment;
import com.vmware.github.domain.ReviewThread;
import com.vmware.util.logging.Padder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ActionDescription("Displays open diff and general comments for pull request review.")
public class DisplayOpenPullRequestReviewComments extends BaseCommitWithPullRequestAction {
    private final Map<String, String[]> diffFiles = new HashMap<>();

    public DisplayOpenPullRequestReviewComments(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        log.debug("Head sha for pull request {}", pullRequest.head.sha);
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd hh:mm:ss");
        Map<String, List<ReviewComment>> commentsPerAuthor = new LinkedHashMap<>();
        ReviewThread[] reviewThreads = github.getReviewThreadsForPullRequest(pullRequest);
        for (ReviewThread reviewThread : reviewThreads) {
            if (reviewThread.isResolved) {
                continue;
            }

            Arrays.stream(reviewThread.comments.nodes)
                    .forEach(comment -> commentsPerAuthor.computeIfAbsent(comment.author.login, key -> new ArrayList<>()).add(comment));
        }

        if (commentsPerAuthor.isEmpty()) {
            log.info("No open comments found for pull request {}", pullRequest.number);
            return;
        }

        for (String author : commentsPerAuthor.keySet()) {
            Padder authorPadder = new Padder(author);
            authorPadder.infoTitle();
            List<ReviewComment> comments = commentsPerAuthor.get(author);
            comments.sort(Comparator.comparing(ReviewComment::getCreatedAt));
            for (ReviewComment comment : comments) {
                Padder notePadder = new Padder(60, formatter.format(comment.createdAt));
                notePadder.infoTitle();
                log.info(comment.diffHunk);
                log.info(comment.body);
                notePadder.infoTitle();
            }
            authorPadder.infoTitle();
        }
    }

}

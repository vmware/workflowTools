package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import java.util.List;

@ActionDescription("Attempts to find a linked changelist by git tag.")
public class SelectLinkedChangelist extends BasePerforceCommitUsingGitAction {
    public SelectLinkedChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isNotEmpty(draft.perforceChangelistId)) {
            return "commit already is linked to changelist " + draft.perforceChangelistId;
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        String headRef = git.revParse("head");
        List<String> tags = git.listTags();
        String matchingTag = tags.stream().filter(tag -> headRef.equals(git.revParse(tag))).findFirst().orElse(null);
        if (matchingTag != null) {
            draft.perforceChangelistId = MatcherUtils.singleMatch(matchingTag, "changeset-(\\d+)");
            log.info("Changelist {} is linked to commit", draft.perforceChangelistId);
        } else {
            log.info("No changelist linked with commit after checking git tags");
        }
    }
}

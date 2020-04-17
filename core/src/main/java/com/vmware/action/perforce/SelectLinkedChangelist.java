package com.vmware.action.perforce;

import java.util.List;

import com.vmware.action.base.BasePerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.MatcherUtils;

import static com.vmware.util.StringUtils.isNotEmpty;

@ActionDescription("Attempts to find a linked changelist by git tag.")
public class SelectLinkedChangelist extends BasePerforceCommitUsingGitAction {
    public SelectLinkedChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(isNotEmpty(draft.perforceChangelistId), "commit already linked with changelist " + draft.perforceChangelistId);
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

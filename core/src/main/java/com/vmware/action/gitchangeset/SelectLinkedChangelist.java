package com.vmware.action.gitchangeset;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.MatcherUtils;

import java.util.List;

@ActionDescription("Attempts to find a linked changelist by git tag.")
public class SelectLinkedChangelist extends BaseCommitAction {
    public SelectLinkedChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        boolean foundMatchingTag = false;
        String headRef = git.revParse("head");
        List<String> tags = git.listTags();
        for (String tag : tags) {
            String tagRef = git.revParse(tag);
            if (headRef.equals(tagRef)) {
                foundMatchingTag = true;
                draft.perforceChangelistId = MatcherUtils.singleMatch(tag, "changeset-(\\d+)");
                break;
            }
        }
        if (foundMatchingTag) {
            log.info("Changelist {} is linked to commit", draft.perforceChangelistId);
        } else {
            log.info("No changelist linked with commit after checking git tags");
        }
    }
}

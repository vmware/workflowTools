package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.exception.FatalException;

import static com.vmware.util.StringUtils.addNewLinesIfNeeded;

@ActionDescription("Formats commit lines to match max lengths.")
public class FormatCommitText extends BaseCommitAction {

    public FormatCommitText(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (draft.summary.length() > commitConfig.maxSummaryLength) {
            throw new FatalException("Commit summary is greater than max length " + commitConfig.maxSummaryLength);
        }

        draft.description = addNewLinesIfNeeded(draft.description, commitConfig.maxDescriptionLength, 0);
        draft.testingDone = addNewLinesIfNeeded(draft.testingDone, commitConfig.maxDescriptionLength,
                commitConfig.testingDoneLabel.length());
    }
}

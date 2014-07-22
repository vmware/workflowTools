package com.vmware.action.commitInfo;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Formats commit lines to match max lengths.")
public class FormatCommitText extends AbstractCommitAction {

    public FormatCommitText(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (draft.summary.length() > config.maxSummaryLength) {
            throw new IllegalArgumentException("Commit summary is greater than max length " + config.maxSummaryLength);
        }

        draft.description = StringUtils.addNewLinesIfNeeded(draft.description,
                config.maxDescriptionLength, 0);
        draft.testingDone = StringUtils.addNewLinesIfNeeded(draft.testingDone,
                config.maxDescriptionLength, config.testingDoneLabel.length());
    }
}

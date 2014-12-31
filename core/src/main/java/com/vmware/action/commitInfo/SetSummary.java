package com.vmware.action.commitInfo;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.input.InputUtils;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Sets the summary field. Replaces existing value if there is one.")
public class SetSummary extends AbstractCommitAction {

    public SetSummary(WorkflowConfig config) {
        super(config);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException {
        if (!config.setEmptyPropertiesOnly || StringUtils.isBlank(draft.summary)) {
            return true;
        }

        log.info("Skipping action {} as setEmptyPropertiesOnly is set to true and Summary has a value",
                this.getClass().getSimpleName());
        return false;
    }

    @Override
    public void process() throws IOException {
        if (StringUtils.isNotBlank(draft.summary)) {
            log.info("Existing Summary\n" + draft.summary);
        }

        String topic = InputUtils.readData("Topic (defaults to " + config.defaultTopic + " if none set)",
                true, 20, config.topicTemplates);
        topic = topic.isEmpty() ? config.defaultTopic : topic;
        draft.summary = topic + ": " + InputUtils.readData("Enter Summary", true, config.maxSummaryLength - (topic.length() + 2));
    }
}

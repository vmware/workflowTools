package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ActionDescription("Sets the summary field. Replaces existing value if there is one.")
public class SetSummary extends BaseCommitAction {

    public SetSummary(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(commitConfig.setEmptyPropertiesOnly && StringUtils.isNotEmpty(draft.summary),
                "setEmptyPropertiesOnly is set to true and Summary has a value");
    }

    @Override
    public void process() {
        List<String> autoSuggestValues = new ArrayList<>();
        if (StringUtils.isNotEmpty(draft.summary)) {
            log.info("Existing Summary\n" + draft.summary);
            autoSuggestValues.add(draft.topic());
        }

        autoSuggestValues.addAll(Arrays.asList(commitConfig.topicTemplates));



        String topic = InputUtils.readData("Topic (defaults to " + commitConfig.defaultTopic + " if none set)",
                true, 20, autoSuggestValues.toArray(new String[0]));
        topic = topic.isEmpty() ? commitConfig.defaultTopic : topic;
        draft.summary = topic + ": " + InputUtils.readData("Enter Summary", true, commitConfig.maxSummaryLength - (topic.length() + 2), draft.summaryWithoutTopic());
    }
}

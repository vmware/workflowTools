package com.vmware.action.commitInfo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;

@ActionDescription("Set explicit merge to value")
public class SetMergeTo extends BaseCommitAction {

    public SetMergeTo(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String mergeToText = InputUtils.readValue("Enter comma separated merge to values");
        String[] mergeToValues = Arrays.stream(mergeToText.split(",")).map(String::trim).map(this::appendValueIfNeeded).toArray(String[]::new);
        log.info("Merge to values are {}", Arrays.toString(mergeToValues));
        draft.mergeToValues = mergeToValues;
    }

    private String appendValueIfNeeded(String value) {
        if (value.contains(":")) {
            return value;
        }
        return value + commitConfig.mergeToDefault;
    }
}

package com.vmware.action.filesystem;

import java.util.Properties;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;
import com.vmware.util.MatcherUtils;

@ActionDescription("Parses text with supplied regex. Sets first matched group to output variable")
public class ParseText extends BaseAction {
    public ParseText(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("inputText", "regex", "outputVariableName");
    }

    @Override
    public void process() {
        log.info("Parsing {} with regex {}", fileSystemConfig.inputText, fileSystemConfig.regex);
        String matchedValue = MatcherUtils.singleMatchExpected(fileSystemConfig.inputText, fileSystemConfig.regex);
        log.info("Setting variable {} with value {}", fileSystemConfig.outputVariableName, matchedValue);
        replacementVariables.addVariable(fileSystemConfig.outputVariableName, matchedValue, false);
    }
}

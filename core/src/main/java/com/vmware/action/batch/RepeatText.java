package com.vmware.action.batch;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

import java.util.stream.IntStream;

@ActionDescription("Repeat the file data the specified number of times")
public class RepeatText extends BaseAction {
    public RepeatText(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("fileData");
    }

    @Override
    public void process() {
        StringBuilder builder = new StringBuilder();
        IntStream.rangeClosed(1, fileSystemConfig.repeatCount).forEach(index -> {
            String massagedData = fileSystemConfig.fileData.replace("$counter", String.valueOf(index));
            builder.append(massagedData);
        });

        log.info(builder.toString());
        log.info("Copying to clipboard");
        SystemUtils.copyTextToClipboard(builder.toString());
    }
}

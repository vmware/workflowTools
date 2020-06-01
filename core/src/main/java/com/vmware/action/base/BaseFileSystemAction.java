package com.vmware.action.base;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

public abstract class BaseFileSystemAction extends BaseAction {

    protected String fileData;
    private boolean fileDataShouldBeSet;

    protected BaseFileSystemAction(WorkflowConfig config) {
        this(config, true);
    }

    protected BaseFileSystemAction(WorkflowConfig config, boolean fileDataShouldBeSet) {
        super(config);
        this.fileDataShouldBeSet = fileDataShouldBeSet;
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (fileDataShouldBeSet) {
            failIfTrue(StringUtils.isEmpty(fileData), "no file data set");
        }

    }

    public void setFileData(String fileData) {
        this.fileData = fileData;
    }

    public String getFileData() {
        return fileData;
    }
}

package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

@ActionDescription("Commits all staged files to a commit. Input text can be set for the description.")
public class SimpleCommit extends BaseAction {
    public SimpleCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd HH:mm:ss zzz");
        String commitMessage = StringUtils.isNotBlank(fileSystemConfig.inputText) ? fileSystemConfig.inputText :
                "Commit created at " + formatter.format(new Date());
        git.commit(commitMessage, gitRepoConfig.noVerify);
    }
}

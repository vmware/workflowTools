package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

@ActionDescription("Commits all files to a commit. Input text can be set for the description.")
public class SimpleCommitAll extends BaseAction {
    public SimpleCommitAll(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd HH:mm:ss zzz");
        String commitMessage = StringUtils.isNotBlank(fileSystemConfig.inputText) ? fileSystemConfig.inputText :
                "Latest Commit at " + formatter.format(new Date());
        git.commitWithAllFileChanges(commitMessage, gitRepoConfig.noVerify);
    }
}

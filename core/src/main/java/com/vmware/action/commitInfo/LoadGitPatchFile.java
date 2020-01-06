package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.RepoType;
import com.vmware.util.FileUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

import java.io.File;

@ActionDescription("Used to load a git patch file that can then be applied as a git patch.")
public class LoadGitPatchFile extends BaseCommitAction {
    public LoadGitPatchFile(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String diffFilePath = patchConfig.diffFilePath;
        if (StringUtils.isEmpty(diffFilePath)) {
            diffFilePath = InputUtils.readValueUntilNotBlank("Git diff path");
        }
        draft.repoType = RepoType.git;
        draft.draftPatchData = FileUtils.readFileAsString(new File(diffFilePath));
    }
}

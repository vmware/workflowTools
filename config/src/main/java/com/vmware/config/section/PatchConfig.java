package com.vmware.config.section;

import com.vmware.config.ActionAfterFailedPatchCheck;
import com.vmware.config.ConfigurableProperty;

public class PatchConfig {

    @ConfigurableProperty(commandLine = "--diff-file", help = "Diff file to load")
    public String diffFilePath;

    @ConfigurableProperty(commandLine = "--action-after-failed-patch-check", help = "What to do if checking the patch failed, valid values are nothing, partial or usePatchCommand")
    public ActionAfterFailedPatchCheck actionAfterFailedPatchCheck;

    @ConfigurableProperty(commandLine = "--use-patch-command", help = "Use patch command to apply diff")
    public boolean usePatchCommand;

    @ConfigurableProperty(commandLine = "--patch-command", help = "Patch command to use to apply patch")
    public String patchCommand;

    @ConfigurableProperty(commandLine = "--latest-diff", help = "Always use latest diff from review request for patching")
    public boolean alwaysUseLatestDiff;
}

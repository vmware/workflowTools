package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Select a backup directory to use")
public class SelectBackup  extends BaseAction {
    public SelectBackup(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile", "backupFilePattern", "outputVariableName");
    }

    @Override
    public void process() {
        File[] backupDirectories = new File(fileSystemConfig.sourceFile).listFiles(this::matchesBackupPattern);
        if (backupDirectories == null || backupDirectories.length == 0) {
            throw new FatalException("No backups found for pattern {} in directory {}", fileSystemConfig.backupFilePattern, fileSystemConfig.sourceFile);
        }

        List<String> backupDirectoryNames = Arrays.stream(backupDirectories).map(File::getName).collect(Collectors.toList());
        log.debug("Found backup directories {} for pattern {}", backupDirectoryNames, fileSystemConfig.backupFilePattern);

        List<String> backupDirectoryDisplayNames = backupDirectoryNames.stream().map(value -> MatcherUtils.singleMatchExpected(value, fileSystemConfig.backupFilePattern)).collect(Collectors.toList());
        int selectedBackup = InputUtils.readSelection(backupDirectoryDisplayNames, "Select backup");
        String backupValue = fileSystemConfig.sourceFile + File.separator + backupDirectoryNames.get(selectedBackup);

        replacementVariables.addVariable(fileSystemConfig.outputVariableName, backupValue);
    }

    private boolean matchesBackupPattern(File file) {
        log.debug("Checking name {} against pattern {}", file.getName(), fileSystemConfig.backupFilePattern);
        return file.getName().matches(fileSystemConfig.backupFilePattern);
    }
}

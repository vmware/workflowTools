package com.vmware.action.filesystem;

import java.io.File;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Backups source file / directory to destination file / directory.")
public class BackupFile extends BaseAction {
    public BackupFile(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile", "destinationFile");
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(fileSystemConfig.skipBackup, "skipBackup is set to true");
    }

    @Override
    public void process() {
        String sourceFilePath = fileSystemConfig.sourceFile;
        String destinationFilePath = fileSystemConfig.destinationFile;

        log.debug("Configured source file {} and destination file {}", sourceFilePath, destinationFilePath);

        File fileToCopy = new File(sourceFilePath);
        File destinationFile = new File(destinationFilePath);

        if (destinationFile.exists() && !fileSystemConfig.overwriteBackup) {
            log.info("{} already exists. --overwrite-backup flag can be used to overwrite automatically, --skip-backup to skip backing up", destinationFile.getAbsolutePath());

            String backupFile = InputUtils.readValueUntilNotBlank("Action? (o)verwrite (s)kip (c)ancel", "yes", "no");
            if ("skip".startsWith(backupFile)) {
                log.info("Skipping backing up of {}", sourceFilePath);
                return;
            } else if (!"skip".startsWith(backupFile)) {
                cancelWithMessage("{} already exists", destinationFile.getAbsolutePath());
            }
        }

        if (fileToCopy.isDirectory()) {
            log.info("Backing up directory {} to {}", fileToCopy.getAbsolutePath(), destinationFile.getAbsolutePath());
            FileUtils.copyDirectory(fileToCopy, destinationFile);
        } else {
            log.info("Backing up file {} to {}", fileToCopy.getAbsolutePath(), destinationFile.getAbsolutePath());
            FileUtils.copyFile(fileToCopy, destinationFile);
        }
    }
}

package com.vmware.action.filesystem;

import java.io.File;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;

import static com.vmware.util.StringUtils.isNotEmpty;

@ActionDescription("Creates a backup of specified file. Uses the same directory if no destination file is set. File can be either a file or directory.")
public class BackupFile extends BaseAction {
    public BackupFile(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile");
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(fileSystemConfig.skipBackup, "skipBackup is set to true");
    }

    @Override
    public void process() {
        String sourceFilePath = git.addRepoDirectoryIfNeeded(fileSystemConfig.sourceFile);
        String destinationFilePath = git.addRepoDirectoryIfNeeded(fileSystemConfig.destinationFile);

        log.debug("Configured source file {} and destination file {}", sourceFilePath, destinationFilePath);

        File fileToBackup = new File(sourceFilePath);
        String defaultBackupPath = fileToBackup.getParentFile().getPath() + File.separator + fileToBackup.getName() + "." + fileSystemConfig.backupName;
        File destinationFile = isNotEmpty(destinationFilePath) ? new File(destinationFilePath) : new File(defaultBackupPath);

        if (destinationFile.exists()) {
            cancelWithErrorMessage(destinationFile.getAbsolutePath() + " already exists. Use --skip-backup if backup is not needed");
        }

        if (fileToBackup.isDirectory()) {
            log.info("Backing up directory {} to {}", fileToBackup.getAbsolutePath(), destinationFile.getAbsolutePath());
            FileUtils.copyDirectory(fileToBackup, destinationFile);
        } else {
            log.info("Backing up file {} to {}", fileToBackup.getAbsolutePath(), destinationFile.getAbsolutePath());
            FileUtils.copyFile(fileToBackup, destinationFile);
        }
    }
}

package com.vmware.action.filesystem;

import java.io.File;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;

import static com.vmware.util.StringUtils.isNotEmpty;

@ActionDescription("Copies source file / directory to destination file / directory.")
public class CopyFile extends BaseAction {
    public CopyFile(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile", "destinationFile");
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(fileSystemConfig.skipFileCopy, "skipFileCopy is set to true");
    }

    @Override
    public void process() {
        String sourceFilePath = fileSystemConfig.sourceFile;
        String destinationFilePath = fileSystemConfig.destinationFile;

        log.debug("Configured source file {} and destination file {}", sourceFilePath, destinationFilePath);

        File fileToCopy = new File(sourceFilePath);
        File destinationFile = new File(destinationFilePath);

        if (destinationFile.exists() && !fileSystemConfig.replaceExisting) {
            log.info("{} already exists. --replace-existing flag can be used to overwrite automatically", destinationFile.getAbsolutePath());

            String replaceFile =InputUtils.readValueUntilNotBlank("Replace " + destinationFile.getAbsolutePath() + " (yes/no)", "yes", "no");
            if (!"yes".equalsIgnoreCase(replaceFile)) {
                cancelWithMessage("{} already exists", destinationFile.getAbsolutePath());
            }
        }

        if (fileToCopy.isDirectory()) {
            log.info("Copying directory {} to {}", fileToCopy.getAbsolutePath(), destinationFile.getAbsolutePath());
            FileUtils.copyDirectory(fileToCopy, destinationFile);
        } else {
            log.info("Copying file {} to {}", fileToCopy.getAbsolutePath(), destinationFile.getAbsolutePath());
            FileUtils.copyFile(fileToCopy, destinationFile);
        }
    }
}

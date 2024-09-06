package com.vmware;

import com.google.gson.Gson;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.logging.WorkflowConsoleHandler;
import com.vmware.util.scm.Git;
import com.vmware.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Add last commit info from commit to the internal config json file.
 */
public class AddBuildInfoToInternalConfig {

    private static final String CREATION_DATE = "Creation Date";

    private static final Logger log = LoggerFactory.getLogger(AddBuildInfoToInternalConfig.class);

    public static void main(String[] args) throws FileNotFoundException {
        LogManager.getLogManager().reset();
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        globalLogger.addHandler(new WorkflowConsoleHandler());
        globalLogger.setLevel(Level.INFO);

        if (args.length == 0) {
            log.error("Module base directory not specified");
            System.exit(1);
        }

        String moduleBaseDirectory = args[0];
        String sourceConfigJsonFilePath = moduleBaseDirectory + "/src/main/resources/internalConfig.json";
        File sourceConfigJsonFile = new File(sourceConfigJsonFilePath);
        String targetConfigJsonFilePath = moduleBaseDirectory + "/target/classes/internalConfig.json";
        File targetConfigJsonFile = new File(targetConfigJsonFilePath);


        if (!Git.isGitInstalled()) {
            log.warn("Git is not installed, cannot add git version info");
            return;
        }

        Git git = new Git(new File(moduleBaseDirectory));
        log.info("Loading internalConfig.json");
        Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();

        WorkflowConfig workflowConfig = gson.fromJson(new FileReader(sourceConfigJsonFile), WorkflowConfig.class);

        workflowConfig.buildInfo = git.getLastCommitInfo();
        workflowConfig.buildInfo.put(CREATION_DATE, new Date().toString());
        log.info("Adding last commit info\n{}", workflowConfig.buildInfo);
        String jsonOutput = gson.toJson(workflowConfig);
        log.info("Saving to target config file {}", targetConfigJsonFilePath);
        IOUtils.write(targetConfigJsonFile, jsonOutput);
    }
}

package com.vmware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.vmware.util.CommandLineUtils.executeCommand;

/**
 * Common functionality for both git and perforce wrappers can be put in this superclass.
 */
public abstract class BaseScmWrapper {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected final File workingDirectory;

    public BaseScmWrapper(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    protected String executeScmCommand(String command) {
        return executeScmCommand(command, false);
    }

    protected String executeScmCommand(String command, boolean printLines) {
        return executeScmCommand(command, null, printLines);
    }

    protected String executeScmCommand(String command, String inputText, boolean printLines) {
        log.debug("{} command {}", this.getClass().getSimpleName(), command);
        String output = executeCommand(workingDirectory, command, inputText, printLines);
        exitIfCommandFailed(output);
        return output;
    }

    protected void exitIfCommandFailed(String output) {
    }

}

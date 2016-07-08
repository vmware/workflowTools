package com.vmware;

import com.vmware.util.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.vmware.util.CommandLineUtils.executeCommand;

/**
 * Common functionality for both git and perforce wrappers can be put in this superclass.
 */
public abstract class BaseScmWrapper {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected File workingDirectory;

    protected void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    protected String executeScmCommand(String command, String... arguments) {
        return executeScmCommand(command, LogLevel.DEBUG, arguments);
    }

    protected String executeScmCommand(String command, LogLevel logLevel, String... commandArguments) {
        return executeScmCommand(command, null, logLevel, commandArguments);
    }

    protected String executeScmCommand(String command, String inputText, LogLevel level, String... commandArguments) {
        String expandedCommand = expandCommand(command, commandArguments);
        log.debug("{} command {}", this.getClass().getSimpleName(), expandedCommand);
        String output = executeCommand(workingDirectory, expandedCommand, inputText, level);
        exitIfCommandFailed(output);
        return output;
    }

    protected void exitIfCommandFailed(String output) {
    }

    private String expandCommand(String command, String... arguments) {
        if (arguments.length == 0) {
            return command;
        }
        for (String argument : arguments) {
            command = command.replaceFirst("\\{\\}", argument);
        }
        return command;
    }

}

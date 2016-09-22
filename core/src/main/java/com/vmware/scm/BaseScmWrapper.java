package com.vmware.scm;

import com.vmware.util.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import static com.vmware.util.CommandLineUtils.executeCommand;

/**
 * Common functionality for both git and perforce wrappers can be put in this superclass.
 */
public abstract class BaseScmWrapper {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    File workingDirectory;

    ScmType scmType;

    BaseScmWrapper(ScmType scmType) {
        this.scmType = scmType;
    }

    void setWorkingDirectory(File workingDirectory) {
        if (workingDirectory == null) {
            throw new IllegalArgumentException("Cannot set null working directory for client "
                    + this.getClass().getSimpleName());
        }
        this.workingDirectory = workingDirectory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public String fullPath(String pathWithinScm) {
        return workingDirectory + File.separator + pathWithinScm;
    }

    String executeScmCommand(String command, String... arguments) {
        return executeScmCommand(command, LogLevel.DEBUG, arguments);
    }

    String executeScmCommand(String command, LogLevel logLevel, String... commandArguments) {
        return executeScmCommand(command, null, logLevel, commandArguments);
    }

    String executeScmCommand(String command, String inputText, LogLevel level, String... commandArguments) {
        return executeScmCommand(null, command, inputText, level, commandArguments);
    }

    String executeScmCommand(Map<String, String> environmentVariables, String command, String inputText, LogLevel level, String... commandArguments) {
        String expandedCommand = expandCommand(command, commandArguments);
        log.debug("{} command {}", this.getClass().getSimpleName(), expandedCommand);
        String output = executeCommand(workingDirectory, environmentVariables, expandedCommand, inputText, level);
        exitIfCommandFailed(output);
        return output;
    }

    void exitIfCommandFailed(String output) {
    }

    String failOutputIfMissingText(String output, String expectedText) {
        if (!output.contains(expectedText)) {
            throw new RuntimeException("Expcted to find text " + expectedText + " in output " + output);
        }
        return output;
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

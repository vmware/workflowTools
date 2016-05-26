package com.vmware;

import com.vmware.util.IOUtils;
import com.vmware.util.Padder;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Common functionality for both git and perforce wrappers can be put in this superclass.
 */
public class AbstractScmWrapper {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected final File workingDirectory;

    public AbstractScmWrapper(File workingDirectory) {
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
        String output = executeCommand(command, inputText, printLines);
        exitIfCommandFailed(output);
        return output;
    }

    protected String executeCommand(String command, String inputText, boolean printLines) {
        ProcessBuilder builder = new ProcessBuilder(command.split(" ")).directory(workingDirectory)
                .redirectErrorStream(true);
        try {
            Process statusProcess = builder.start();
            if (inputText != null) {
                IOUtils.write(statusProcess.getOutputStream(), inputText);
            }
            return readProcessOutput(statusProcess.getInputStream(), printLines);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    protected String readProcessOutput(InputStream input, boolean printLines) {
        Padder titlePadder = new Padder(this.getClass().getSimpleName() + " Output");
        Level logLevel = printLines ? Level.INFO : Level.FINEST;

        titlePadder.logTitle(logLevel);
        String output = IOUtils.read(input);
        if (printLines) {
            log.info(output);
        } else {
            log.trace(output);
        }
        titlePadder.logTitle(logLevel);
        return output;
    }

    protected void exitIfCommandFailed(String output) {
    }

}

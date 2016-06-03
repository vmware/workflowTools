package com.vmware.util;

import com.vmware.util.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Used for easy use of executing commands
 */
public class CommandLineUtils {

    private static Logger log = LoggerFactory.getLogger(CommandLineUtils.class);

    public static boolean isCommandAvailable(String command) {
        String osName = System.getProperty("os.name");
        log.debug("Os name {}", osName);
        if (osName == null) {
            return false;
        } else if (osName.startsWith("Windows")) {
            String whereCheck = executeCommand(null, "where " + command, null, false);
            log.debug("{} {} where check [{}]", osName, command, whereCheck);
            return !whereCheck.contains("Could not find files");
        } else {
            String whichCheck = executeCommand(null, "which " + command, null, false);
            log.debug("{} {} which check [{}]", osName, command, whichCheck);
            return !whichCheck.trim().isEmpty();
        }
    }

    public static String executeCommand(File workingDirectory, String command, String inputText, boolean printLines) {
        ProcessBuilder builder = new ProcessBuilder(command.split(" ")).directory(workingDirectory)
                .redirectErrorStream(true);
        try {
            Process statusProcess = builder.start();
            if (inputText != null) {
                IOUtils.write(statusProcess.getOutputStream(), inputText);
            }
            return readProcessOutput(command, statusProcess.getInputStream(), printLines);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private static String readProcessOutput(String command, InputStream input, boolean printLines) {
        Padder titlePadder = new Padder(command);
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
}

package com.vmware.util;

import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Used for easy use of executing commands
 */
public class CommandLineUtils {

    private static Logger log = LoggerFactory.getLogger(CommandLineUtils.class);
    private static DynamicLogger dynamicLogger = new DynamicLogger(log);

    public static boolean isCommandAvailable(String command) {
        String osName = System.getProperty("os.name");
        log.debug("Os name {}", osName);
        if (osName == null) {
            return false;
        } else if (osName.startsWith("Windows")) {
            String whereCheck = executeCommand(null, "where " + command, null, LogLevel.TRACE);
            log.debug("{} {} where check [{}]", osName, command, whereCheck);
            return !whereCheck.contains("Could not find files");
        } else {
            String whichCheck = executeCommand(null, "which " + command, null, LogLevel.TRACE);
            log.debug("{} {} which check [{}]", osName, command, whichCheck);
            return !whichCheck.trim().isEmpty();
        }
    }

    public static String executeCommand(String command, LogLevel logLevel) {
        return executeCommand(null, command, null, logLevel);
    }


    public static String executeCommand(File workingDirectory, String command, String inputText, LogLevel logLevel) {
        return executeCommand(workingDirectory, null, command, inputText, logLevel);
    }

    public static String executeCommand(File workingDirectory, Map<String, String> environmentVariables,
                                        String command, String inputText, LogLevel logLevel) {
        ProcessBuilder builder = new ProcessBuilder(command.split(" ")).directory(workingDirectory)
                .redirectErrorStream(true);
        if (environmentVariables != null) {
            builder.environment().putAll(environmentVariables);
        }
        Date startingDate = new Date();
        Padder titlePadder = new Padder(command);
        titlePadder.logTitle(logLevel);
        Process statusProcess = executeCommand(workingDirectory, environmentVariables, command, inputText);

        String output = IOUtils.read(statusProcess.getInputStream(), logLevel);
        long elapsedMilliseconds = new Date().getTime() - startingDate.getTime();
        long elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMilliseconds);
        LogLevel elapsedTimeLogLevel = elapsedTimeInSeconds > 1 ? logLevel : LogLevel.DEBUG;
        String plural = elapsedTimeInSeconds == 1 ? "" : "s";
        dynamicLogger.log(elapsedTimeLogLevel, "Execution time {} second{}", String.valueOf(elapsedTimeInSeconds), plural);
        titlePadder.logTitle(logLevel);
        return output;
    }

    public static Process executeCommand(File workingDirectory, Map<String, String> environmentVariables,
                                        String command, String inputText) {
        ProcessBuilder builder = new ProcessBuilder(command.split(" ")).directory(workingDirectory)
                .redirectErrorStream(true);
        if (environmentVariables != null) {
            builder.environment().putAll(environmentVariables);
        }
        try {
            Process statusProcess = builder.start();
            if (inputText != null) {
                IOUtils.write(statusProcess.getOutputStream(), inputText);
            }
            return statusProcess;
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }


    public static String executeScript(String command, String[] inputs, String[] textsToWaitFor, LogLevel logLevel) {
        log.info("Executing script {}", command);
        ProcessBuilder builder = new ProcessBuilder(command.split(" ")).redirectErrorStream(true);
        String totalOutput = "";
        try {
            Process statusProcess = builder.start();
            for (int i = 0; i < textsToWaitFor.length; i ++) {
                long sleepTime = 0;
                boolean matchedText = false;
                while (sleepTime < TimeUnit.SECONDS.toMillis(30) && !matchedText) {
                    String output = IOUtils.readWithoutClosing(statusProcess.getInputStream());
                    if (StringUtils.isNotBlank(output)) {
                        dynamicLogger.log(logLevel, output);
                    }
                    if (!totalOutput.isEmpty()) {
                        totalOutput += "\n";
                    }
                    totalOutput += output;
                    matchedText = output.trim().contains(textsToWaitFor[i]);
                    if (matchedText && inputs.length > i) {
                        dynamicLogger.log(logLevel, "Found {} in output, writing [{}]", textsToWaitFor[i], inputs[i]);
                        totalOutput += inputs[i];
                        IOUtils.writeWithoutClosing(statusProcess.getOutputStream(), inputs[i] + "\n");
                        sleepTime = 0;
                    } else if (matchedText) {
                        dynamicLogger.log(logLevel, "Found {} in output", textsToWaitFor[i]);
                        sleepTime = 0;
                    } else {
                        sleepTime += TimeUnit.SECONDS.toMillis(2);
                        ThreadUtils.sleep(2, TimeUnit.SECONDS);
                    }
                }
                if (!matchedText) {
                    throw new IllegalArgumentException("Failed to match " + textsToWaitFor[i] + " in script output " + totalOutput);
                }

            }
            return totalOutput;
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}

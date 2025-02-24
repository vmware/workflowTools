package com.vmware.util;

import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Used for easy use of executing commands
 */
public class CommandLineUtils {

    private static final Logger log = LoggerFactory.getLogger(CommandLineUtils.class);
    private static final DynamicLogger dynamicLogger = new DynamicLogger(log);

    public static boolean isOsxCommandAvailable(String command) {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        boolean isOsx = osName.contains("mac") || osName.contains("darwin");
        if (!isOsx) {
            return false;
        }
        return checkForCommandUsingWhich(command);
    }

    public static boolean isCommandAvailable(String command) {
        String osName = System.getProperty("os.name");
        log.trace("Os name {}", osName);
        if (osName == null) {
            return false;
        } else if (osName.startsWith("Windows")) {
            return checkForCommandUsingWhere(command);
        } else {
            return checkForCommandUsingWhich(command);
        }
    }

    private static boolean checkForCommandUsingWhere(String command) {
        try {
            String whereCheck = executeCommand(null, "where " + command, null, true, LogLevel.TRACE);
            log.debug("{} where check [{}]", command, whereCheck);
            return !whereCheck.contains("Could not find files");
        } catch (FatalException fe) {
            log.debug("where {} failed with output {}", command, fe.getMessage());
            return false;
        }
    }

    private static boolean checkForCommandUsingWhich(String command) {
        try {
            String whichCheck = executeCommand(null, "which " + command, null, true, LogLevel.TRACE);
            log.debug("{} which check [{}]", command, whichCheck);
            return !whichCheck.trim().isEmpty() && !whichCheck.contains("no " + command + " in");
        } catch (FatalException fe) {
            log.debug("which {} failed with output {}", command, fe.getMessage());
            return false;
        }
    }

    public static String executeCommand(String command, LogLevel logLevel) {
        return executeCommand(null, command, null, false, logLevel);
    }

    public static String executeCommand(File workingDirectory, String command, String inputText, boolean logResultOnly, LogLevel logLevel) {
        return executeCommand(workingDirectory, null, command, inputText, logResultOnly, logLevel);
    }

    public static String executeCommand(File workingDirectory, Map<String, String> environmentVariables,
                                        String command, String inputText, boolean logResultOnly, LogLevel logLevel) {
        Padder titlePadder = new Padder(command);
        if (!logLevel.isDebug()) {
            dynamicLogger.log(logLevel, "Executing command {}", command);
        } else if (command.length() > titlePadder.getMaxTitleLength()) {
            dynamicLogger.log(logLevel, "Executing command {}", command);
        }
        ProcessBuilder builder = new ProcessBuilder(splitCommand(command)).directory(workingDirectory)
                .redirectErrorStream(true);
        if (environmentVariables != null) {
            builder.environment().putAll(environmentVariables);
        }
        StopwatchUtils.Stopwatch stopwatch = StopwatchUtils.start();
        if (!logResultOnly) {
            titlePadder.logTitle(logLevel);
        }
        Process statusProcess = executeCommand(workingDirectory, environmentVariables, command, inputText);

        String output = IOUtils.read(statusProcess.getInputStream(), logResultOnly ? null : logLevel);
        if (logResultOnly && StringUtils.isNotBlank(output)) {
            dynamicLogger.log(logLevel, "Executed command [{}] Output [{}]", command, output);
        } else if (logResultOnly) {
            dynamicLogger.log(logLevel, "Executed command {}", command);
        }
        long elapsedMilliseconds = stopwatch.elapsedTime();
        if (elapsedMilliseconds > 50 && elapsedMilliseconds < 2000) {
            dynamicLogger.log(LogLevel.DEBUG, "Execution time {} milliseconds", elapsedMilliseconds);
        } else if (elapsedMilliseconds >= 2000) {
            long elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMilliseconds);
            dynamicLogger.log(LogLevel.INFO, "Execution time {} seconds for command {}", elapsedTimeInSeconds, command);
        }

        if (!logResultOnly) {
            titlePadder.logTitle(logLevel);
        }
        try {
            statusProcess.waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (statusProcess.exitValue() != 0) {
            throw new FatalException("Command {} failed with status code {}: {}", command, statusProcess.exitValue(), output);
        }
        return output;
    }

    public static Process executeCommand(File workingDirectory, Map<String, String> environmentVariables,
                                        String command, String inputText) {
        ProcessBuilder builder = new ProcessBuilder(splitCommand(command)).directory(workingDirectory)
                .redirectErrorStream(true);
        if (environmentVariables != null) {
            builder.environment().putAll(environmentVariables);
        }

        Process statusProcess = startProcess(builder);
        if (inputText != null) {
            IOUtils.write(statusProcess.getOutputStream(), inputText);
        }
        return statusProcess;
    }

    public static String executeScript(String command, String[] inputs, String[] textsToWaitFor, LogLevel logLevel) {
        log.info("Executing script {}", command);
        ProcessBuilder builder = new ProcessBuilder(splitCommand(command)).redirectErrorStream(true);
        String totalOutput = "";

        Process statusProcess = startProcess(builder);

        for (int i = 0; i < textsToWaitFor.length; i ++) {
            long sleepTime = 0;
            boolean matchedText = false;
            long maxSleepTimeInMillis = TimeUnit.SECONDS.toMillis(30);
            while (sleepTime < maxSleepTimeInMillis && !matchedText) {
                String output = IOUtils.readWithoutClosing(statusProcess.getInputStream());
                if (StringUtils.isNotEmpty(output)) {
                    dynamicLogger.log(logLevel, output);
                }
                totalOutput = StringUtils.appendWithDelimiter(totalOutput, output, "\n");
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
                    sleepTime += ThreadUtils.sleep(2, TimeUnit.SECONDS);
                }
            }
            if (!matchedText) {
                throw new FatalException("Failed to match {} in script output {}", textsToWaitFor[i], totalOutput);
            }

        }
        return totalOutput;

    }

    private static Process startProcess(ProcessBuilder builder) {
        Process statusProcess;
        try {
            statusProcess = builder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return statusProcess;
    }

    private static String[] splitCommand(String command) {
        command += " "; // To detect last token when not quoted...
        ArrayList<String> strings = new ArrayList<String>();
        boolean inQuote = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < command.length(); i++) {
            char character = command.charAt(i);
            if (character == '"' || character == ' ' && !inQuote) {
                if (character == '"') {
                    inQuote = !inQuote;
                }
                if (!inQuote && sb.length() > 0) {
                    strings.add(sb.toString());
                    sb.delete(0, sb.length());
                }
            } else {
                sb.append(character);
            }
        }
        return strings.toArray(new String[0]);
    }
}

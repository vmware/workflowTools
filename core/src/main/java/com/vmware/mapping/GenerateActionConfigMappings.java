package com.vmware.mapping;

import com.vmware.SimpleLogFormatter;
import com.vmware.config.ConfigurableProperty;
import com.vmware.config.WorkflowConfig;
import com.vmware.rest.json.ConfiguredGsonBuilder;
import com.vmware.utils.FileUtils;
import com.vmware.utils.IOUtils;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class in run by the build to generate the action config mappings.
 * Specifically it creates a json file where each action has a list of config values that are relevant for that action.
 * This is done by a regex pattern that matches "config.[configValueName]
 * This works because the workflow config class is expected to be called config in all the action classes.
 */
public class GenerateActionConfigMappings {

    private static final Logger log = LoggerFactory.getLogger(GenerateActionConfigMappings.class);

    private static final Pattern configValuePattern = Pattern.compile("[^\\.]config\\.(\\w+)");

    private Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();

    private File actionDirectory;

    private File outputFile;


    public GenerateActionConfigMappings(String actionDirectory, String outputFile) {
        this.actionDirectory = new File(actionDirectory);
        this.outputFile = new File(outputFile);
    }

    public void run() throws IOException {
        List<File> javaActionFiles = FileUtils.scanDirectorRecursivelyForFiles(actionDirectory, new JavaFileFilter());
        if (javaActionFiles.isEmpty()) {
            log.info("No action files found at {}", actionDirectory.getPath());
            return;
        }

        WorkflowConfig config = new WorkflowConfig();
        config.generateConfigurablePropertyList();

        Map<String, String[]> mappings = new HashMap<String, String[]>();
        for (File javaActionFile : javaActionFiles) {
            String className = FileUtils.stripExtension(javaActionFile);
            List<String> foundConfigValues = parseFileForConfigValues(javaActionFile);
            List<String> commandLineOptions = convertToCommandLineValues(foundConfigValues, config.configurableFields);
            if (!commandLineOptions.isEmpty()) {
                mappings.put(className, commandLineOptions.toArray(new String[commandLineOptions.size()]));
            }
        }
        String jsonOutput = gson.toJson(mappings);
        IOUtils.write(outputFile, jsonOutput);
    }

    private List<String> convertToCommandLineValues(List<String> foundConfigValues, List<Field> configurableFields) {
        List<String> commandLineOptions = new ArrayList<String>();

        for (int i = foundConfigValues.size() -1; i >= 0; i --) {
            String foundConfigValue = foundConfigValues.get(i);
            for (Field configurableField : configurableFields) {
                if (configurableField.getName().equals(foundConfigValue)) {
                    ConfigurableProperty configurableProperty = configurableField.getAnnotation(ConfigurableProperty.class);
                    String commandLineText = configurableProperty.commandLine();
                    if (!commandLineText.equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                        String[] commandLineArgs = commandLineText.split(",");
                        commandLineOptions.add(commandLineArgs[commandLineArgs.length - 1]);
                    }
                    break;
                }
            }
        }
        return commandLineOptions;
    }

    private List<String> parseFileForConfigValues(File javaActionFile) throws IOException {
        String fileText = IOUtils.read(javaActionFile);
        Matcher configMatcher = configValuePattern.matcher(fileText);
        List<String> matches = new ArrayList<String>();
        while (configMatcher.find()) {
            String foundValue = configMatcher.group(1);
            if (!matches.contains(foundValue)) {
                matches.add(configMatcher.group(1));
            }
        }
        return matches;
    }


    private class JavaFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathName) {
            return pathName.isDirectory() || pathName.getName().endsWith(".java");
        }
    }

    public static void main(String[] args) throws IOException {
        LogManager.getLogManager().reset();
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        globalLogger.addHandler(createHandler());
        globalLogger.setLevel(Level.INFO);

        if (args.length == 0) {
            log.error("Module base directory not specified");
            System.exit(1);
        }

        String moduleBaseDirectory = args[0];
        String actionsDirectory = moduleBaseDirectory + "/src/main/java/com/vmware/action";
        String mappingsFile = moduleBaseDirectory + "/src/main/resources/configValueMappings.json";
        String targetDirectory = moduleBaseDirectory + "/target/classes";

        actionsDirectory = actionsDirectory.replaceAll("/", File.separator);
        mappingsFile = mappingsFile.replaceAll("/", File.separator);
        targetDirectory = targetDirectory.replaceAll("/", File.separator);

        log.info("Creating mappings file {} from folder {}", mappingsFile, actionsDirectory);

        GenerateActionConfigMappings generateActionConfigMappings =
                new GenerateActionConfigMappings(actionsDirectory, mappingsFile);
        generateActionConfigMappings.run();
        log.info("Created mappings file");
        log.info("Copying to target directory {}", targetDirectory);
        FileUtils.copyFile(new File(mappingsFile),
                new File(targetDirectory + File.separator + "configValueMappings.json"));
    }

    private static ConsoleHandler createHandler() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleLogFormatter());
        handler.setLevel(Level.FINEST);
        return handler;
    }
}

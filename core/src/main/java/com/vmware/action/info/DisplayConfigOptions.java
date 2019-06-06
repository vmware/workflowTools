package com.vmware.action.info;

import com.google.gson.Gson;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ConfigurableProperty;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowField;
import com.vmware.config.WorkflowFields;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.ClasspathResource;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.logging.Padder;
import com.vmware.util.StringUtils;

import java.io.Reader;
import java.util.Arrays;
import java.util.Map;

@ActionDescription("Displays a list of configuration options that can be set.")
public class DisplayConfigOptions extends BaseAction {

    private final Gson gson;

    public DisplayConfigOptions(WorkflowConfig config) {
        super(config);
        gson = new ConfiguredGsonBuilder().build();
    }

    @Override
    public void process() {
        Reader reader = new ClasspathResource("/internalConfig.json", this.getClass()).getReader();
        WorkflowConfig defaultConfig = gson.fromJson(reader, WorkflowConfig.class);
        log.info("");
        log.info("Printing configuration options");
        log.info("Each option is displayed in the following format");
        log.info("name, command line overrides if any, description, default value if any");
        log.info("");
        log.info("Each option can also be specified as a git config value, prepend workflow. to the name");
        log.info("E.g. workflow.configFile would be the git config value for configuring the configuration file");
        log.info("");
        log.info("Overriding config option priority from lowest to highest: \n{}\n{}\n{}\n{}\n{}",
                "Project Config file", "User Config file", "Git Config Value", "Specified Config Files", "Command Line Arguments");
        Padder titlePadder = new Padder("Configuration Options");
        titlePadder.infoTitle();
        log.info("configFile, [-c,--config] Optional configuration file to use, file is in json format, Defaults to config file in jar");
        WorkflowFields configurableFields = config.getConfigurableFields();
        for (WorkflowField field : configurableFields.values()) {
            ConfigurableProperty configProperty = field.configAnnotation();
            Object defaultValue = field.getValue(defaultConfig);

            String defaultDisplayValue;
            if (defaultValue == null) {
              defaultDisplayValue = null;
            } else if (Map.class.isAssignableFrom(field.getType())) {
                defaultDisplayValue = ((Map) defaultValue).keySet().toString();
            } else if (field.getType() == int[].class) {
                defaultDisplayValue = Arrays.toString((int[]) defaultValue);
            } else if (field.getType().isArray()) {
                defaultDisplayValue = Arrays.toString((Object[]) defaultValue);
            } else {
                defaultDisplayValue = String.valueOf(defaultValue);
            }

            String defaultDisplayText = defaultDisplayValue != null ? "Default: " + defaultDisplayValue: "No Default";
            log.info("{},[{}], {}, {}", field.getName(), configProperty.commandLine(), configProperty.help(), defaultDisplayText);
            if (StringUtils.isNotBlank(configProperty.gitConfigProperty())) {
                log.info("{} can also be set from git config value {}", field.getName(), configProperty.gitConfigProperty());
            }
        }
        titlePadder.infoTitle();
    }
}

package com.vmware.action.info;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;

@ActionDescription("Displays the current workflow configuration.")
public class DisplayConfig extends AbstractAction {

    public DisplayConfig(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        log.info("Loaded config file {}", config.loadedConfigFiles);

        Padder titlePadder = new Padder("Workflow Configuration");
        titlePadder.infoTitle();
        for (int i = 0; i < config.configurableFields.size(); i ++) {
            Field configField = config.configurableFields.get(i);
            String source = config.overriddenConfigSources.get(configField.getName());
            source = source == null ? "Internal Config" : source;
            String displayValue = determineDisplayValue(configField.get(config));
            log.info("{} ({}) - {}", configField.getName(), source, displayValue);
            if (i < config.configurableFields.size() - 1) {
                log.info("");
            }
        }
        titlePadder.infoTitle();
    }

    private String determineDisplayValue(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof int[]) {
            return Arrays.toString((int[]) value);
        } else if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        } else if (value instanceof Map) {
            Map values = (Map) value;
            String displayText = "";
            for (Object key : values.keySet()) {
                Object displayValue = determineDisplayValue(values.get(key));
                displayText += "\n" + key + " " + displayValue;
            }
            return displayText;
        } else {
            return value.toString();
        }

    }
}

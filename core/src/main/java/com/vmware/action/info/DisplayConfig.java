package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;
import com.vmware.util.exception.RuntimeReflectiveOperationException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ActionDescription("Displays the current workflow configuration.")
public class DisplayConfig extends BaseAction {

    public DisplayConfig(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Loaded config files {}", config.loadedConfigFiles);

        Padder titlePadder = new Padder("Workflow Configuration");
        titlePadder.infoTitle();
        List<String> specifiedPropertiesToDisplay = new ArrayList<>();
        if (StringUtils.isNotBlank(config.configPropertiesToDisplay)) {
            specifiedPropertiesToDisplay.addAll(Arrays.asList(config.configPropertiesToDisplay.split(",")));
        }
        boolean printingFirstValue = false;
        for (int i = 0; i < config.configurableFields.size(); i ++) {
            Field configField = config.configurableFields.get(i);
            if (!showValueForProperty(specifiedPropertiesToDisplay, configField.getName())) {
                continue;
            }
            String source = config.overriddenConfigSources.get(configField.getName());
            source = source == null ? "default" : source;
            String displayValue = null;
            try {
                displayValue = determineDisplayValue(configField.get(config));
            } catch (IllegalAccessException e) {
                throw new RuntimeReflectiveOperationException(e);
            }
            if (!printingFirstValue) {
                printingFirstValue = true;
            } else {
                log.info("");
            }
            log.info("{} ({}) - {}", configField.getName(), source, displayValue);
        }
        titlePadder.infoTitle();
    }

    private boolean showValueForProperty(List<String> specifiedProperties, String propertyName) {
        if (specifiedProperties.isEmpty()) {
            return true;
        }
        for (String specifiedProperty : specifiedProperties) {
            if (specifiedProperty.equalsIgnoreCase(propertyName)) {
                return true;
            }
        }
        return false;
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

package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowField;
import com.vmware.config.WorkflowFields;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ActionDescription("Displays the current workflow configuration.")
public class DisplayConfig extends BaseAction {

    public DisplayConfig(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Loaded config files {}", config.getConfigurableFields().loadedConfigFilesText());
        Padder titlePadder = new Padder("Workflow Configuration");
        titlePadder.infoTitle();
        List<String> specifiedPropertiesToDisplay = new ArrayList<>();
        if (StringUtils.isNotEmpty(config.configPropertiesToDisplay)) {
            specifiedPropertiesToDisplay.addAll(Arrays.asList(config.configPropertiesToDisplay.split(",")));
        }
        boolean printingFirstValue = false;
        Set<String> alreadyProcessedProperties = new HashSet<>();
        WorkflowFields fields = config.getConfigurableFields();
        for (int i = 0; i < fields.size(); i++) {
            WorkflowField configField = fields.get(i);
            if (!showValueForProperty(specifiedPropertiesToDisplay, configField.getName(), alreadyProcessedProperties)) {
                continue;
            }
            String source = fields.getFieldValueSource(configField.getName());
            String displayValue = configField.getDisplayValue(config);
            if (!printingFirstValue) {
                printingFirstValue = true;
            } else {
                log.info("");
            }
            log.info("{} ({}) - {}", configField.getName(), source, displayValue);
        }
        titlePadder.infoTitle();
    }

    private boolean showValueForProperty(List<String> specifiedProperties, String propertyName, Set<String> alreadyProcessedProperties) {
        if (alreadyProcessedProperties.contains(propertyName)) {
            return false;
        }
        alreadyProcessedProperties.add(propertyName);
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
}

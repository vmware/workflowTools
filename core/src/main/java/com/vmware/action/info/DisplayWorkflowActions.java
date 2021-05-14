package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowActions;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.Padder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@ActionDescription("Displays a list of workflow actions that can be executed.")
public class DisplayWorkflowActions extends BaseAction {

    public DisplayWorkflowActions(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Padder titlePadder = new Padder("Workflow Actions");
        titlePadder.infoTitle();

        Map<String, List<Class<? extends BaseAction>>> classMap = generateClassMap();
        for (String packageName : classMap.keySet()) {
            Padder packagePadder = new Padder(convertToReadableText(packageName));
            packagePadder.infoTitle();
            List<Class<? extends BaseAction>> classes = classMap.get(packageName);
            for (Class<? extends BaseAction> action : classes) {
                ActionDescription description = action.getAnnotation(ActionDescription.class);
                if (description == null) {
                    throw new RuntimeException("Please add a action description annotation for " + action.getSimpleName());
                }
                String helpText = "- " + description.value();
                if (!helpText.endsWith(".")) {
                    helpText += ".";
                }
                log.info(action.getSimpleName() + " " + helpText);
            }
            packagePadder.infoTitle();
        }
        titlePadder.infoTitle();
    }

    private Map<String, List<Class<? extends BaseAction>>> generateClassMap() {
        Map<String, List<Class<? extends BaseAction>>> classes = new TreeMap<String, List<Class<? extends BaseAction>>>();
        List<Class<? extends BaseAction>> workflowActions = new WorkflowActions(config, WorkflowConfig.appClassLoader).getWorkflowActionClasses();
        for (Class<? extends BaseAction> action : workflowActions) {
            String[] pieces = action.getName().split("\\.");
            String packageName = pieces[pieces.length - 2];
            if (!classes.containsKey(packageName)) {
                classes.put(packageName, new ArrayList<Class<? extends BaseAction>>());
            }
            classes.get(packageName).add(action);
        }
        return classes;
    }

    public String convertToReadableText(String value) {
        char[] chars = value.toCharArray();
        StringBuilder readableValue = new StringBuilder();
        for (char character : chars) {
            if (Character.isUpperCase(character) && (readableValue.length() > 0)) {
                readableValue.append(" ").append(character);
            } else if (readableValue.length() == 0) {
                readableValue.append(Character.toUpperCase(character));
            } else {
                readableValue.append(character);
            }
        }
        return readableValue.toString();
    }
}

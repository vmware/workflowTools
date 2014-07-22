package com.vmware.action.info;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@ActionDescription("Displays a list of workflow actions that can be executed.")
public class DisplayWorkflowActions extends AbstractAction {

    public DisplayWorkflowActions(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        Padder titlePadder = new Padder("Workflow Actions");
        titlePadder.infoTitle();

        Map<String, List<Class<? extends AbstractAction>>> classMap = generateClassMap();
        for (String packageName : classMap.keySet()) {
            Padder packagePadder = new Padder(convertToReadableText(packageName));
            packagePadder.infoTitle();
            List<Class<? extends AbstractAction>> classes = classMap.get(packageName);
            for (Class<? extends AbstractAction> action : classes) {
                ActionDescription description = action.getAnnotation(ActionDescription.class);
                if (description == null) {
                    throw new RuntimeException("Please add a action description annotation for " + action.getSimpleName());
                }
                String helpText = "- " + description.value();
                log.info(action.getSimpleName() + " " + helpText);
            }
            packagePadder.infoTitle();
        }
        titlePadder.infoTitle();
    }

    private Map<String, List<Class<? extends AbstractAction>>> generateClassMap() {
        Map<String, List<Class<? extends AbstractAction>>> classes = new TreeMap<String, List<Class<? extends AbstractAction>>>();
        for (int i = 0; i < config.workFlowActions.size(); i ++) {
            Class<? extends AbstractAction> action = config.workFlowActions.get(i);
            String[] pieces = action.getName().split("\\.");
            String packageName = pieces[pieces.length - 2];
            if (!classes.containsKey(packageName)) {
                classes.put(packageName, new ArrayList<Class<? extends AbstractAction>>());
            }
            classes.get(packageName).add(action);
        }
        return classes;
    }

    public String convertToReadableText(String value) {
        char[] chars = value.toCharArray();
        String readableValue = "";
        for (char character : chars) {
            if (Character.isUpperCase(character) && !readableValue.isEmpty()) {
                readableValue += " " + character;
            } else if (readableValue.isEmpty()) {
                readableValue += Character.toUpperCase(character);
            } else {
                readableValue += character;
            }
        }
        return readableValue;
    }
}

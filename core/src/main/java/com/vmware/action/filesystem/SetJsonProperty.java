package com.vmware.action.filesystem;

import java.io.StringReader;
import java.util.Map;

import com.google.gson.Gson;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.exception.FatalException;

@ActionDescription(value = "Sets value for a specified json property value.", configFlagsToExcludeFromCompleter = "--file-data")
public class SetJsonProperty extends DeleteJsonProperty {
    public SetJsonProperty(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("jsonPropertyPath", "propertyValue");
    }

    @Override
    public void process() {
        log.info("Setting json property {} to {}", fileSystemConfig.jsonPropertyPath, fileSystemConfig.propertyValue);

        Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();
        Map<String, Object> jsonMap = gson.fromJson(new StringReader(fileSystemConfig.fileData), Map.class);

        JsonMatch jsonMatch = findJsonPropertyMatch(jsonMap, fileSystemConfig.jsonPropertyPath);
        if (jsonMatch == null) {
            throw new FatalException("Failed to find match for path " + fileSystemConfig.jsonPropertyPath);
        }
        jsonMatch.setValue(fileSystemConfig.propertyValue);
        fileSystemConfig.fileData = gson.toJson(jsonMap);
    }
}

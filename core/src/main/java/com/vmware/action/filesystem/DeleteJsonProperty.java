package com.vmware.action.filesystem;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

@ActionDescription("Deletes the specified property path in the source json file.")
public class DeleteJsonProperty extends BaseAction {
    public DeleteJsonProperty(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("fileData", "jsonPropertyPath");
    }

    @Override
    public void process() {
        log.info("Deleting json property path {}", fileSystemConfig.jsonPropertyPath);

        Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();
        Map<String, Object> jsonMap = gson.fromJson(new StringReader(fileSystemConfig.fileData), Map.class);

        JsonMatch jsonMatch = findJsonPropertyMatch(jsonMap, fileSystemConfig.jsonPropertyPath);
        if (jsonMatch == null) {
            log.info("Could not find a match for {} so skipping deletion of json property path", fileSystemConfig.jsonPropertyPath);
            return;
        }
        jsonMatch.removeMatch();
        fileSystemConfig.fileData = gson.toJson(jsonMap);
    }

    protected JsonMatch findJsonPropertyMatch(Map<String, Object> jsonMap, String propertyPath) {
        Map<String, Object> propertiesToSearch = jsonMap;
        List<String> propertyPaths = StringUtils.splitAndTrim(propertyPath, "\\.");
        StringBuilder alreadyMatchedParts = new StringBuilder();
        Object matchedPart = null;
        for (int i = 0; i < propertyPaths.size(); i ++) {
            String fullPropertyName = propertyPaths.get(i);
            String propertyPathToSearchFor = fullPropertyName;
            Integer propertyIndex = null;
            if (propertyPathToSearchFor.contains("[")) {
                propertyIndex = Integer.parseInt(MatcherUtils.singleMatchExpected(propertyPathToSearchFor, "\\[(\\d+)\\]"));
                propertyPathToSearchFor = propertyPathToSearchFor.substring(0, propertyPathToSearchFor.indexOf("["));
            }
            matchedPart = propertiesToSearch.get(propertyPathToSearchFor);
            if (propertyIndex != null && !(matchedPart instanceof List)) {
                throw new FatalException("Path {}.{} could not be found", alreadyMatchedParts.toString(), fullPropertyName);
            }
            if (matchedPart instanceof List) {
                List parts = (List) matchedPart;
                if (propertyIndex != null && parts.size() <= propertyIndex) {
                    log.info("No match for index {}, largest index is {}", propertyIndex, parts.size() - 1);
                    return null;
                } else {
                    matchedPart = parts.get(propertyIndex != null ? propertyIndex : 0);
                }
            }
            if (matchedPart == null && i < propertyPaths.size() - 1) {
                log.info("Path {}.{} could not be found", alreadyMatchedParts.toString(), propertyPathToSearchFor);
                return null;
            } else if (matchedPart == null && i == propertyPaths.size() - 1) {
                log.info("Property {} was missing for {}", fullPropertyName, alreadyMatchedParts.toString());
                return null;
            }
            if (i < propertyPaths.size() - 1 && !(matchedPart instanceof Map)) {
                throw new FatalException("Path {}.{} was of type {}, should be a map",
                        alreadyMatchedParts.toString(), fullPropertyName, matchedPart.getClass().getName());
            }
            if (i > 0) {
                alreadyMatchedParts.append(".");
            }
            alreadyMatchedParts.append(fullPropertyName);
            if (i < propertyPaths.size() - 1) {
                propertiesToSearch = (Map<String, Object>) matchedPart;
            } else {
                return new JsonMatch(propertiesToSearch, propertyPathToSearchFor, propertyIndex);
            }
        }
        return null;
    }

    protected class JsonMatch {
        private Map parentObject;
        private String matchingPropertyName;
        private Integer matchingIndex;

        public JsonMatch(Map parentObject, String matchingPropertyName, Integer matchingIndex) {
            this.parentObject = parentObject;
            this.matchingPropertyName = matchingPropertyName;
            this.matchingIndex = matchingIndex;
        }

        public void setValue(String updatedValue) {
            parentObject.put(matchingPropertyName, updatedValue);
        }

        public void removeMatch() {
            Object removedValue;
            if (matchingIndex != null) {
                List properties = (List) parentObject.get(matchingPropertyName);
                removedValue = properties.remove(matchingIndex.intValue());
            } else {
                removedValue = parentObject.remove(matchingPropertyName);
            }

            if (removedValue == null || Boolean.FALSE.equals(removedValue)) {
                throw new FatalException("No value removed for path " + fileSystemConfig.jsonPropertyPath);
            }
        }
    }
}

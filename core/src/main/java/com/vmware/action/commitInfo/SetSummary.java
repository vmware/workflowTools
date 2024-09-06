package com.vmware.action.commitInfo;

import com.google.gson.Gson;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.IOUtils;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@ActionDescription("Sets the summary field. Replaces existing value if there is one.")
public class SetSummary extends BaseCommitAction {

    public SetSummary(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(commitConfig.setEmptyPropertiesOnly && StringUtils.isNotEmpty(draft.summary),
                "setEmptyPropertiesOnly is set to true and Summary has a value");
    }

    @Override
    public void process() {
        if (StringUtils.isNotEmpty(draft.summary)) {
            log.info("Existing Summary\n" + draft.summary);
        }
        File suggestionsFile = git.getRootDirectory() != null ?
                new File(git.getRootDirectory() + File.separator + ".workflowAutoSuggestions") : null;

        Map<String, List<String>> autoSuggestValues = null;
        Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();
        if (suggestionsFile != null) {
            try {
                autoSuggestValues = gson.fromJson(new FileReader(suggestionsFile), Map.class);
            } catch (FileNotFoundException e) {
                log.debug("File not found {}", suggestionsFile.getPath());
            }
        }
        if (autoSuggestValues == null) {
            autoSuggestValues = new LinkedHashMap<>();
        }

        List<String> topicValuesWithoutTemplates = autoSuggestValues.computeIfAbsent("topic", key -> new ArrayList<>());
        Optional.ofNullable(draft.topic()).filter(StringUtils::isNotBlank).filter(topic -> !topicValuesWithoutTemplates.contains(topic))
                .ifPresent(topic -> topicValuesWithoutTemplates.add(0, topic));

        LinkedHashSet<String> topicValues = new LinkedHashSet<>(topicValuesWithoutTemplates);
        topicValues.addAll(Arrays.asList(commitConfig.topicTemplates));

        String topic = InputUtils.readData("Topic (defaults to " + commitConfig.defaultTopic + " if none set)",
                true, 20, topicValues.toArray(new String[0]));
        topic = topic.isEmpty() ? commitConfig.defaultTopic : topic;
        topicValuesWithoutTemplates.remove(topic);
        topicValuesWithoutTemplates.add(0, topic);

        if (suggestionsFile != null) {
            IntStream.range(5, topicValuesWithoutTemplates.size()).forEach(topicValuesWithoutTemplates::remove);
            IOUtils.write(suggestionsFile, gson.toJson(autoSuggestValues));
        }

        List<String> summaryValues = autoSuggestValues.computeIfAbsent("summary", key -> new ArrayList<>());
        Optional.ofNullable(draft.summaryWithoutTopic()).filter(StringUtils::isNotBlank)
                .filter(value -> !summaryValues.contains(value)).ifPresent(value -> summaryValues.add(0, value));
        String summary = InputUtils.readData("Enter Summary", true, commitConfig.maxSummaryLength - (topic.length() + 2),
                summaryValues.toArray(new String[0]));
        summaryValues.remove(summary);
        summaryValues.add(0, summary);
        draft.summary = topic + ": " + summary;

        if (suggestionsFile != null) {
            IntStream.range(5, summaryValues.size()).forEach(summaryValues::remove);
            IOUtils.write(suggestionsFile, gson.toJson(autoSuggestValues));
        }
    }
}

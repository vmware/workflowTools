package com.vmware;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.google.gson.Gson;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.jenkins.domain.JobDetails;
import com.vmware.util.ClasspathResource;

public class TestJobDefinitionParsing extends BaseTests {

    @Test
    public void canParseDefinition() {
        String definitionText = new ClasspathResource("/jobDefinition.json").getText();
        Gson gson = new ConfiguredGsonBuilder().build();
        JobDetails jobDefinition = gson.fromJson(definitionText, JobDetails.class);
        assertNotNull(jobDefinition);
        assertFalse(jobDefinition.getParameterDefinitions().isEmpty());
    }
}

package com.vmware;

import com.google.gson.Gson;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.jenkins.domain.Job;
import com.vmware.util.ClasspathResource;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class TestJobDefinitionParsing extends BaseTests {

    @Test
    public void canParseDefinition() {
        String definitionText = new ClasspathResource("/jobDefinition.json", this.getClass()).getText();
        Gson gson = new ConfiguredGsonBuilder().build();
        Job jobDefinition = gson.fromJson(definitionText, Job.class);
        assertNotNull(jobDefinition);
        assertFalse(jobDefinition.getParameterDefinitions().isEmpty());
    }
}

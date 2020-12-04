package com.vmware.jenkins.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobParameters {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    public List<JobParameter> parameter = new ArrayList<>();

    public JobParameters(final List<JobParameter> parameters) {
        this.parameter.addAll(parameters);
    }

    public Map<String, Object> toMap() {
        if (parameter == null) {
            return null;
        }
        return parameter.stream().collect(Collectors.toMap(param -> param.name, param -> param.value));
    }

}

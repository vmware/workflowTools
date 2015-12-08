package com.vmware.jenkins.domain;

public class JobParameter {
    public String name;
    public String value;

    public JobParameter(final String name, final String value) {
        this.name = name;
        this.value = value;
    }
}

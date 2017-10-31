package com.vmware.config.jenkins;

public class JobParameter {
    public static final String USERNAME_PARAM = "USERNAME";
    public static final String NO_USERNAME_PARAMETER = "NO_USERNAME_PARAMETER";

    public String name;
    public String value;

    public JobParameter(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

}

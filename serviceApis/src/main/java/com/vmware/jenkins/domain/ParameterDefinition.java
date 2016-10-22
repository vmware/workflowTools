package com.vmware.jenkins.domain;

import com.google.gson.annotations.SerializedName;

public class ParameterDefinition {
    public String name;
    @SerializedName("type")
    public String parameterType;
    public String description;

    public DefaultJobParameterValue defaultParameterValue;

}

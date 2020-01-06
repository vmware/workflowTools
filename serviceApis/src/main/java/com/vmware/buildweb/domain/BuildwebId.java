package com.vmware.buildweb.domain;

import com.vmware.util.StringUtils;

/**
 * Used to encapuslate logic for constructing build id/
 */
public class BuildwebId {

    private String buildType;

    private String buildNumber;

    public BuildwebId(String value) {
        parseBuildId(value);
    }

    private void parseBuildId(String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        String delimeter = value.contains("-") ? "-" : "/";
        String[] buildIdParts = value.split(delimeter);
        if (buildIdParts.length != 2) {
            this.buildType = "sb";
            this.buildNumber = value;
        } else {
            this.buildType = buildIdParts[0];
            this.buildNumber = buildIdParts[1];
        }
    }

    public String getBuildType() {
        return buildType;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public String buildApiPath() {
        return buildType + "/build/" + buildNumber;
    }

    public String buildwebPath() {
        return buildType + "/" + buildNumber;
    }
}

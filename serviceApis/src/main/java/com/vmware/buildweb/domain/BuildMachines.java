package com.vmware.buildweb.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public class BuildMachines {

    @SerializedName("_list")
    public BuildMachine[] buildMachines;

    public BuildMachine realBuildMachine() {
        return Arrays.stream(buildMachines).filter(BuildMachine::nonLauncherMachine).findFirst()
                .orElseThrow(() -> new RuntimeException("No build machine found, build might still be starting"));
    }
}

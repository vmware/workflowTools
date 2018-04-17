package com.vmware.buildweb.domain;

import java.util.Arrays;

import com.google.gson.annotations.SerializedName;

public class BuildMachines {

    @SerializedName("_list")
    public BuildMachine[] buildMachines;

    public BuildMachine realBuildMachine() {
        return Arrays.stream(buildMachines).filter(BuildMachine::nonLauncherMachine).findFirst()
                .orElseThrow(() -> new RuntimeException("No build machine found, build might still be starting"));
    }
}

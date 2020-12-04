package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.util.MatcherUtils;

public class JobBuildArtifact {
    public String fileName;
    public String relativePath;

    public boolean matchesPathPattern(String pathPattern) {
        return MatcherUtils.singleMatch(relativePath, pathPattern) != null;
    }
}

package com.vmware.reviewboard.domain;

import com.vmware.util.exception.InvalidDataException;

public enum RepoType {
    git,
    perforce;

    public static RepoType fromValue(String value) {
        if (value == null) {
            return null;
        }
        value = value.toLowerCase();
        if (value.contains("perforce")) {
            return perforce;
        } else if (value.contains("git")) {
            return git;
        } else {
            throw new InvalidDataException("Repo type value {} not supported", value);
        }
    }
}

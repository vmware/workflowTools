package com.vmware.scm;

import static java.lang.String.format;

public class NoPerforceClientForDirectoryException extends RuntimeException {

    public NoPerforceClientForDirectoryException(String clientRoot, String username, String info) {
        super(format("failed to parse perforce client name for directory %s in clients for user %s from text %s",
                clientRoot, username, info));
    }
}

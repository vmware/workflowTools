package com.vmware.util.scm;

import com.vmware.util.exception.WorkflowRuntimeException;

public class NoPerforceClientForDirectoryException extends WorkflowRuntimeException {

    public NoPerforceClientForDirectoryException(String clientRoot, String username, String info) {
        super("failed to parse perforce client name for directory {} in clients for user {} from text {}",
                clientRoot, username, info);
    }
}

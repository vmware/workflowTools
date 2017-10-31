package com.vmware.http.ssl;

/**
 * Represents an exception thrown by the @see WorkflowCertificateManager
 */
public class WorkflowCertificateException extends RuntimeException {

    public WorkflowCertificateException(String message) {
        super(message);
    }

    public WorkflowCertificateException(Throwable cause) {
        super(cause);
    }
}

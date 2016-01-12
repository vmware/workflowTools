package com.vmware.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Wraps a checked XmlRpcException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeXmlRpcException extends RuntimeException {

    public RuntimeXmlRpcException(XmlRpcException cause) {
        super(cause);
    }
}

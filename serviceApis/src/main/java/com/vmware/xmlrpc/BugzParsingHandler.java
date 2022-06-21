/*
 * Copyright by VMware, Inc. All rights reserved.
 * VMWARE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.vmware.xmlrpc;

import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.parser.XmlRpcResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * This parsing handler is to fix/workaround the "xsi" supporting issue, which
 * is caused by Bugzilla web service when "xsi" tag is returned in XML-RPC
 * result. It should not be returned and does not make any sense to return such
 * things because XML-RPC standard (if there does exist and the WS respects it)
 * never deals with it (refer to bug 902459 for more information).
 * 
 * @author Chao William Zhang, VMware
 */
public class BugzParsingHandler extends XmlRpcResponseParser implements ContentHandler, ErrorHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private XMLReader xr = null;

    public BugzParsingHandler(XMLReader xr, XmlRpcStreamRequestConfig pConfig,
            TypeFactory pTypeFactory) {
        super(pConfig, pTypeFactory);
        this.xr = xr;
    }

    /**
     * Receive notification of the end of an element.
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (localName.equals("Apache2__RequestRec"))
            return;
        super.endElement(uri, localName, qName);
    }

    /**
     * Receive notification of character data.
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (log.isTraceEnabled()) {
            log.trace(new String(ch, start, length));
        }
        super.characters(ch, start, length);
    }

    /**
     * Receive notification of the beginning of an element.
     */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        if (localName.equals("Apache2__RequestRec"))
            return;

        super.startElement(uri, localName, qName, atts);
    }

    /**
     * Begin the scope of a prefix-URI Namespace mapping
     */
    @Override
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        super.startPrefixMapping(prefix, uri);
    }

    // --------Error Handler for debugging use-----------

    /**
     * Receive notification of a recoverable error.
     */
    @Override
    public void error(SAXParseException exception) {
        log.error(exception.getMessage(), exception);
    }

    /**
     * Receive notification of a non-recoverable error.
     * 
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(SAXParseException exception) {
        log.error(exception.getMessage(), exception);
    }

    /**
     * Receive notification of a warning.(non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException exception) {
        log.warn(exception.getMessage(), exception);
    }

}

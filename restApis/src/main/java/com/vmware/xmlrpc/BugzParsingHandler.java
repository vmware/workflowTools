/*
 * Copyright by VMware, Inc. All rights reserved.
 * VMWARE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.vmware.xmlrpc;

import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.parser.XmlRpcResponseParser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xerces.internal.util.NamespaceSupport;

/**
 * This parsing handler is to fix/workaround the "xsi" supporting issue, which
 * is caused by Bugzilla web service when "xsi" tag is returned in XML-RPC
 * result. It should not be returned and does not make any sense to return such
 * things because XML-RPC standard (if there does exist and the WS respects it)
 * never deals with it (refer to bug 902459 for more information).
 * 
 * @author Chao William Zhang, VMware
 */
public class BugzParsingHandler extends XmlRpcResponseParser implements
ContentHandler, ErrorHandler {

    private XMLReader xr = null;

    public BugzParsingHandler(XMLReader xr, XmlRpcStreamRequestConfig pConfig,
            TypeFactory pTypeFactory) {
        super(pConfig, pTypeFactory);
        this.xr = xr;
    }

    /**
     * Receive notification of character data.
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        //System.out.println("characters()-" + new String(ch, start, length));
        super.characters(ch, start, length);
    }

    /**
     * Receive notification of the end of a document.
     */
    @Override
    public void endDocument() throws SAXException {
        // System.out.println("endDocument()");
        super.endDocument();
    }

    /**
     * Receive notification of the end of an element.
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (localName.equals("Apache2__RequestRec"))
            return;
        // System.out.println("endElement()-" + localName);
        super.endElement(uri, localName, qName);
    }

    /**
     * End the scope of a prefix-URI mapping.
     */
    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        // System.out.println("endPrefixMapping()-" + prefix);
        super.endPrefixMapping(prefix);
    }

    /**
     * Receive notification of ignorable whitespace in element content.
     */
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        // System.out.println("ignorableWhitespace()-" + ch);
        super.ignorableWhitespace(ch, start, length);
    }

    /**
     * Receive notification of a processing instruction.
     */
    @Override
    public void processingInstruction(String target, String data)
            throws SAXException {
        // System.out.println("processingInstruction()-" + target + ":" + data);
        super.processingInstruction(target, data);
    }

    /**
     * Receive an object for locating the origin of SAX document events.
     */
    @Override
    public void setDocumentLocator(Locator locator) {
        // System.out.println("setDocumentLocator()-" + locator);
        super.setDocumentLocator(locator);
    }

    /**
     * Receive notification of a skipped entity.
     */
    @Override
    public void skippedEntity(String name) throws SAXException {
        // System.out.println("skippedEntity()-" + name);
        super.skippedEntity(name);
    }

    /**
     * Receive notification of the beginning of a document.
     */
    @Override
    public void startDocument() throws SAXException {
        // System.out.println("startDocument()");

        NamespaceSupport nc = null;
        Object o = xr.getProperty(
                "http://apache.org/xml/properties/internal/namespace-context");
        if (o != null && o instanceof NamespaceSupport) {
            nc = (NamespaceSupport) o;
            nc.declarePrefix("xsi".intern(),
                    "http://www.w3.org/2001/XMLSchema-instance".intern());
        }

        super.startDocument();
    }

    /**
     * Receive notification of the beginning of an element.
     */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        // System.out.println("      uri:[" + uri + ']');
        // System.out.println("localName:[" + localName + ']');
        // System.out.println("    qName:[" + qName + ']');
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
        // System.out.println("startPrefixMapping()-" + prefix + ":" + uri);
        super.startPrefixMapping(prefix, uri);
    }

    // --------Error Handler for debugging use-----------

    /**
     * Receive notification of a recoverable error.
     */
    @Override
    public void error(SAXParseException exception) {
        System.out.println(exception.getMessage());
    }

    /**
     * Receive notification of a non-recoverable error.
     * 
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(SAXParseException exception) {
        System.out.println(exception.getMessage());
    }

    /**
     * Receive notification of a warning.(non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException exception) {
        System.out.println(exception.getMessage());
    }

}

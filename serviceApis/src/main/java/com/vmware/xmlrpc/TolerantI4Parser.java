package com.vmware.xmlrpc;

import org.apache.xmlrpc.parser.AtomicParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Will store numeric value as an long if it is too big for an integer.
 */
public class TolerantI4Parser extends AtomicParser{

    protected void setResult(String pResult) throws SAXException {
        try {
            super.setResult(new Integer(pResult.trim()));
        } catch (NumberFormatException e) {
            try {
                super.setResult(new Long(pResult.trim()));
            } catch (NumberFormatException le) {
                throw new SAXParseException("Failed to parse long value: " + pResult,
                        getDocumentLocator());
            }
        }
    }
}

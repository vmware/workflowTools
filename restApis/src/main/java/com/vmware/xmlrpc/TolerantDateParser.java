package com.vmware.xmlrpc;

import org.apache.xmlrpc.parser.AtomicParser;
import org.apache.xmlrpc.parser.DateParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.text.Format;
import java.text.ParseException;

/**
 * Handle multiple date formats.
 */
public class TolerantDateParser extends AtomicParser {
    private final Format dateFormat;

    /**
     * Creates a new instance with the given format.
     *
     * @param pFormat
     */
    public TolerantDateParser(Format pFormat) {
        this.dateFormat = pFormat;
    }

    protected void setResult(String pResult) throws SAXException {
        String dateValue = pResult.trim();
        if (dateValue.length() == 0) {
            return;
        }
        try {
            if (!dateValue.contains("T")) {
                dateValue = dateValue.replace(" ", "T");
                dateValue = dateValue.replaceAll("-", "");
            }
            super.setResult(dateFormat.parseObject(dateValue));
        } catch (ParseException e) {
            final String msg;
            int offset = e.getErrorOffset();
            if (e.getErrorOffset() == -1) {
                msg = "Failed to parse date value: " + dateValue;
            } else {
                msg = "Failed to parse date value " + dateValue
                        + " at position " + offset;
            }
            throw new SAXParseException(msg, getDocumentLocator(), e);
        }
    }
}

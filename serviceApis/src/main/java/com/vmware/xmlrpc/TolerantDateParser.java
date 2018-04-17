package com.vmware.xmlrpc;

import static java.lang.String.format;

import java.text.Format;
import java.text.ParseException;

import org.apache.xmlrpc.parser.AtomicParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Handle multiple date formats.
 */
public class TolerantDateParser extends AtomicParser {
    private final Format dateFormat;

    /**
     * Creates a new instance with the given format.
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
                msg = format("Failed to parse date value %s at position %s", dateValue, offset);
            }
            throw new SAXParseException(msg, getDocumentLocator(), e);
        }
    }
}

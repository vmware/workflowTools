package com.vmware.xmlrpc;

import java.util.TimeZone;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.DateParser;
import org.apache.xmlrpc.parser.I4Parser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.util.XmlRpcDateTimeDateFormat;

/**
 * Provides parsers that don't barf as easily as the defaults
 */
public class TolerantTypeFactory extends TypeFactoryImpl {
    /**
     * Creates a new instance.
     *
     * @param pController The controller, which operates the type factory.
     */
    public TolerantTypeFactory(XmlRpcController pController) {
        super(pController);
    }

    @Override
    public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
        TypeParser defaultParser = super.getParser(pConfig, pContext, pURI, pLocalName);
        if (defaultParser instanceof I4Parser) {
            return new TolerantI4Parser();
        } else if (defaultParser instanceof DateParser) {
            return new TolerantDateParser(new XmlRpcDateTimeDateFormat(){
                private static final long serialVersionUID = 7585237706442299067L;
                protected TimeZone getTimeZone() {
                    return getController().getConfig().getTimeZone();
                }
            });
        } else {
            return defaultParser;
        }
    }
}

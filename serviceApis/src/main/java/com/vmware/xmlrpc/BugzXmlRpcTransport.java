/*
 * Copyright by VMware, Inc. All rights reserved.
 * VMWARE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.vmware.xmlrpc;

import com.vmware.http.cookie.CookieFileStore;
import com.vmware.utils.IOUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcHttpClientConfig;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransport;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.parser.XmlRpcResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * The implementation is to fix the Search.get_saved_query issue of Bugzilla web
 * service. See bug 902459 for more details.
 *
 * Added support for adding cookies to the requests so that authenticated requests can be called.
 * 
 * @author Chao William Zhang, Damien Biggs, VMware
 */
public class BugzXmlRpcTransport extends XmlRpcSunHttpTransport {

    private final CookieFileStore cookieFileStore;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Creates a new instance.
     */
    public BugzXmlRpcTransport(XmlRpcClient xmlRpcClient, CookieFileStore cookieFileStore) {
        super(xmlRpcClient);
        this.cookieFileStore = cookieFileStore;
    }


    private URLConnection connection;

    @Override
    protected void close() {
         cookieFileStore.addCookiesFromResponse(this.connection);
    }

    @Override
    protected void initHttpHeaders(final XmlRpcRequest pRequest) throws XmlRpcClientException {
        super.initHttpHeaders(pRequest);
        XmlRpcHttpClientConfig config = (XmlRpcHttpClientConfig) pRequest.getConfig();
        URI requestUri;
        try {
            requestUri = config.getServerURL().toURI();
        } catch (URISyntaxException e) {
            throw new XmlRpcClientException(e.getMessage(), e);
        }
        setRequestHeader("Cookie", cookieFileStore.toCookieRequestText(requestUri.getHost(), false));
    }

    @Override
    protected URLConnection getURLConnection() {
        return this.connection;
    }

    /**
     * Special handling with Search.get_saved_query requests.
     * 
     * @see org.apache.xmlrpc.client.XmlRpcTransport#sendRequest(org.apache.xmlrpc.XmlRpcRequest)
     */
    @Override
    public Object sendRequest(XmlRpcRequest pRequest) throws XmlRpcException {
        // This call is on super class chain so must be executed earlier here.
        XmlRpcHttpClientConfig config = (XmlRpcHttpClientConfig) pRequest.getConfig();
        try {
            final URLConnection c = connection = newURLConnection(config.getServerURL());
            c.setUseCaches(false);
            c.setDoInput(true);
            c.setDoOutput(true);
        } catch (IOException e) {
            throw new XmlRpcException("Failed to create URLConnection: " + e.getMessage(), e);
        }
        initHttpHeaders(pRequest);
        boolean closed = false;
        try {
            ReqWriter reqWriter = newReqWriter(pRequest);
            writeRequest(reqWriter);
            InputStream istream = getInputStream();

            if (isResponseGzipCompressed(config))
                istream = new GZIPInputStream(istream);

            // special handling for Search.get_saved_query to work around bugs.
            String rpcMethod = pRequest.getMethodName();
            byte[] buf = new byte[1024000];
            if (rpcMethod.equals("Search.get_saved_query")) {
                System.out.println(
                        "Special handling for Search.get_saved_query...");
                istream.read(buf);
                String bufStr = new String(buf);
                String resultStr = null;
                int idx1 = 0;
                int idx2 = 0;
                idx1 = bufStr.indexOf("SELECT");
                if (idx1 >= 0) {
                    idx2 = bufStr.indexOf("</string>", idx1);
                    if (idx2 >= 0)
                        resultStr = bufStr.substring(idx1, idx2)
                        + " LIMIT 5000";
                    // System.out.println(resultStr);
                }
                return resultStr;
            }

            Object result = readResponse(config, istream);
            closed = true;
            close();
            return result;
        } catch (IOException e) {
            throw new XmlRpcException("Failed to read server's response: "
                    + e.getMessage(), e);
        } catch (SAXException e) {
            Exception ex = e.getException();
            if (ex != null && ex instanceof XmlRpcException)
                throw (XmlRpcException) ex;
            throw new XmlRpcException("Failed to generate request: "
                    + e.getMessage(), e);
        } finally {
            if (!closed)
                try {
                    close();
                } catch (Throwable ignore) {
                }
        }
    }

    /**
     * Override to fix the xsi (XMLSchema-instance) name space which is not
     * supported by XML-RPC, otherwise will throw exception.
     * 
     * @see org.apache.xmlrpc.client.XmlRpcStreamTransport#readResponse(org.apache.xmlrpc.common.XmlRpcStreamRequestConfig,
     *      java.io.InputStream)
     */
    @Override
    protected Object readResponse(XmlRpcStreamRequestConfig pConfig,
            InputStream pStream) throws XmlRpcException {
        XMLReader xr = newXMLReader();
        XmlRpcResponseParser xp;

        try {
            // no use:
            // xr.setProperty(Constants.XERCES_PROPERTY_PREFIX +
            // Constants.BUFFER_SIZE_PROPERTY, new Integer(62000));

            xp = new BugzParsingHandler(xr, pConfig, getClient()
                    .getTypeFactory());
            xr.setContentHandler(xp);
            if (log.isTraceEnabled()) {
                String content = IOUtils.read(pStream);
                log.trace("Response text from server\n{}", content);
                xr.parse(new InputSource(new StringReader(content)));
            } else {
                xr.parse(new InputSource(pStream));
            }

        } catch (SAXException e) {
            throw new XmlRpcClientException(
                    "Failed to parse server's response: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new XmlRpcClientException(
                    "Failed to read server's response: " + e.getMessage(), e);
        }
        if (xp.isSuccess())
            return xp.getResult();
        Throwable t = xp.getErrorCause();
        if (t == null)
            throw new XmlRpcException(xp.getErrorCode(), xp.getErrorMessage());
        if (t instanceof XmlRpcException)
            throw (XmlRpcException) t;
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        throw new XmlRpcException(xp.getErrorCode(), xp.getErrorMessage(), t);
    }
}

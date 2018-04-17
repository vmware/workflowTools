package com.vmware.xmlrpc;

import java.net.CookieStore;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;

import com.vmware.http.cookie.CookieFileStore;

/**
 * A cookie-aware implementation of an
 * {@link org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory}, that uses a
 * {@link CookieStore} to handle login information over application lifecycle.
 * The cookie store has to be provided when constructing the object.
 * 
 * @author Sebastian Kirchner
 *
 * 
 */
class XmlRpcCookiesTransportFactory extends XmlRpcCommonsTransportFactory {

	private CookieFileStore cookieStore = null;

	private XmlRpcClient xmlRpcClient = null;

	/**
	 * @param xmlRpcClient
	 *            the client, which is controlling the factory.
	 * @param cookieStore
	 *            store, that should be used to manage the cookies (eg. login
	 *            cookie) of the url connection over the application runtime
	 */
	public XmlRpcCookiesTransportFactory(final XmlRpcClient xmlRpcClient,
			final CookieFileStore cookieStore) {
		super(xmlRpcClient);
		this.xmlRpcClient = xmlRpcClient;
		this.cookieStore = cookieStore;
	}

	@Override
	public XmlRpcTransport getTransport() {
        return new BugzXmlRpcTransport(xmlRpcClient, cookieStore);
    }
}

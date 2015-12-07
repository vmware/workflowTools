/**
 * 
 */
package com.vmware.xmlrpc;

import com.vmware.rest.cookie.CookieFileStore;
import com.vmware.rest.exception.NotAuthorizedException;
import com.vmware.rest.exception.NotFoundException;
import com.vmware.rest.ssl.WorkflowCertificateManager;
import com.vmware.utils.input.InputUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Eine Version {@link org.apache.xmlrpc.client.XmlRpcClient} mit Cookie-Verwaltung.
 * 
 * @author Sebastian Kirchner
 */
public class CookieAwareXmlRpcClient extends org.apache.xmlrpc.client.XmlRpcClient {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private CookieFileStore cookieFileStore;
    private WorkflowCertificateManager workflowCertificateManager = null;

	public CookieAwareXmlRpcClient(final URL apiURL) throws IOException, URISyntaxException {
		super();

        String homeFolder = System.getProperty("user.home");
        cookieFileStore = new CookieFileStore(homeFolder);
        workflowCertificateManager = new WorkflowCertificateManager(homeFolder + "/.workflowTool.keystore");
        askIfSslCertShouldBeSaved(apiURL.toURI());

		final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(apiURL);

		final XmlRpcCommonsTransportFactory factory = new XmlRpcCookiesTransportFactory(this, cookieFileStore);
		this.setTransportFactory(factory);
        this.setTypeFactory(new TolerantTypeFactory(this));
		this.setConfig(config);
	}

    public <T> T executeCall(String methodName, Object... params) throws IOException {
        try {
            return (T) super.execute(methodName, params);
        } catch (XmlRpcException e) {
            if (e.getMessage().contains("No profiles object could be found")
                    || e.getMessage().contains("Login Required")) {
                throw new NotAuthorizedException(e.getMessage());
            } else if (e.getMessage().contains("Bug #") && e.getMessage().contains("does not exist.")) {
                throw new NotFoundException(e.getMessage());
            }
            throw new IOException(e);
        }
    }

    private void askIfSslCertShouldBeSaved(URI uri) throws IOException {
        if (workflowCertificateManager == null || workflowCertificateManager.urlAlreadyTrusted(uri)) {
            return;
        }
        log.info("Host {} is not trusted, do you want to save the cert for this to the local trust store {}",
                uri.getHost(), workflowCertificateManager.getKeyStore());
        log.warn("NB: ONLY save the certificate if you trust the host shown");
        String response = InputUtils.readValue("Save certificate? [Y/N] ");
        if ("Y".equalsIgnoreCase(response)) {
            workflowCertificateManager.saveCertForUri(uri);
        }
    }

}

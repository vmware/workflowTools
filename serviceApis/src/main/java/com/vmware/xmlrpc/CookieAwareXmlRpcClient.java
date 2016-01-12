/**
 * 
 */
package com.vmware.xmlrpc;

import com.vmware.http.cookie.CookieFileStore;
import com.vmware.http.exception.InternalServerException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.ssl.WorkflowCertificateManager;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.exception.RuntimeURISyntaxException;
import com.vmware.util.input.InputUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Eine Version {@link XmlRpcClient} mit Cookie-Verwaltung.
 * 
 * @author Sebastian Kirchner
 */
public class CookieAwareXmlRpcClient extends XmlRpcClient {

    private final URL apiURL;
    private Logger log = LoggerFactory.getLogger(this.getClass());

	private CookieFileStore cookieFileStore;
    private WorkflowCertificateManager workflowCertificateManager = null;

	public CookieAwareXmlRpcClient(String url) {
		super();

        try {
            this.apiURL = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeIOException(e);
        }
        String homeFolder = System.getProperty("user.home");
        cookieFileStore = new CookieFileStore(homeFolder);
        workflowCertificateManager = new WorkflowCertificateManager(homeFolder + "/.workflowTool.keystore");

		final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(apiURL);

		final XmlRpcCommonsTransportFactory factory = new XmlRpcCookiesTransportFactory(this, cookieFileStore);
		this.setTransportFactory(factory);
        this.setTypeFactory(new TolerantTypeFactory(this));
		this.setConfig(config);
	}

    public <T> T executeCall(String methodName, Object... params) {
        try {
            askIfSslCertShouldBeSaved(apiURL.toURI());
            return (T) super.execute(methodName, params);
        } catch (XmlRpcException e) {
            if (e.getMessage().contains("No profiles object could be found")
                    || e.getMessage().contains("Login Required")) {
                throw new NotAuthorizedException(e.getMessage());
            } else if (e.getMessage().contains("does not exist.")) {
                throw new NotFoundException(e.getMessage());
            }
            throw new RuntimeXmlRpcException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeURISyntaxException(e);
        }
    }

    public boolean isUriTrusted(URI uri) {
        return workflowCertificateManager.isUriTrusted(uri);
    }

    private void askIfSslCertShouldBeSaved(URI uri) {
        if (workflowCertificateManager == null || workflowCertificateManager.isUriTrusted(uri)) {
            return;
        }
        log.info("Host {} is not trusted, do you want to save the cert for this to the local workflow trust store {}",
                uri.getHost(), workflowCertificateManager.getKeyStore());
        log.warn("NB: ONLY save the certificate if you trust the host shown");
        String response = InputUtils.readValue("Save certificate? [Y/N] ");
        if ("Y".equalsIgnoreCase(response)) {
            workflowCertificateManager.saveCertForUri(uri);
        }
    }

}

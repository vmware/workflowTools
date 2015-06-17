package com.vmware;

import com.vmware.rest.ssl.WorkflowCertificateManager;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for workflow cert manager
 */
public class TestWorkflowCertManager extends BaseTests {

    private WorkflowCertificateManager workflowCertificateManager;

    @Before
    public void initCertManager() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        File keystoreFile = File.createTempFile("test", ".keystore");
        keystoreFile.deleteOnExit();
        workflowCertificateManager = new WorkflowCertificateManager(keystoreFile.getPath());
    }

    /**
     * Google uses a valid ssl cert so shouldn't need saving of it's cert.
     */
    @Test
    public void canTrustGoogleUri() throws IOException, NoSuchAlgorithmException {
        assertTrue("https://www.google.com was not trusted!",
                workflowCertificateManager.urlAlreadyTrusted(URI.create("https://google.com")));
    }

    @Test
    public void cannotTrustJiraUri() throws IOException, NoSuchAlgorithmException {
        String jiraUrl = testProperties.getProperty("jira.url");
        assertFalse(jiraUrl + " was trusted!",
                workflowCertificateManager.urlAlreadyTrusted(URI.create(jiraUrl)));
    }

    @Test
    public void canTrustJiraUriAfterSavingCert() throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        String jiraUrl = testProperties.getProperty("jira.url");
        URI uriToTest = URI.create(jiraUrl);
        assertFalse(jiraUrl + " was trusted!",
                workflowCertificateManager.urlAlreadyTrusted(uriToTest));
        workflowCertificateManager.saveCertForUri(uriToTest);
    }
}

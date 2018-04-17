package com.vmware;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.junit.Before;
import org.junit.Test;

import com.vmware.http.ssl.WorkflowCertificateManager;

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
                workflowCertificateManager.isUriTrusted(URI.create("https://google.com")));
    }

    @Test
    public void cannotTrustJiraUri() throws IOException, NoSuchAlgorithmException {
        String jiraUrl = testProperties.getProperty("jira.url");
        assertFalse(jiraUrl + " was trusted!",
                workflowCertificateManager.isUriTrusted(URI.create(jiraUrl)));
    }

    @Test
    public void canTrustJiraUriAfterSavingCert() throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        String jiraUrl = testProperties.getProperty("jira.url");
        URI uriToTest = URI.create(jiraUrl);
        assertFalse(jiraUrl + " was trusted!",
                workflowCertificateManager.isUriTrusted(uriToTest));
        workflowCertificateManager.saveCertForUri(uriToTest);
    }
}

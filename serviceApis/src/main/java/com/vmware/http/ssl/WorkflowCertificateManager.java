package com.vmware.http.ssl;

import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class used to add the server's certificate to the KeyStore
 * with your trusted certificates.
 */
public class WorkflowCertificateManager {

    private static final char[] PASS_PHRASE = "workflowKeystore".toCharArray();

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private File keyStoreFile;
    private KeyStore workflowKeystore;
    private SavingTrustManager workflowTrustManager;
    private SSLContext context;

    private Set<String> trustedHosts = new HashSet<>();

    public WorkflowCertificateManager() {
        this(null);
    }

    public WorkflowCertificateManager(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile != null ? new File(keyStoreFile) : null;
        try {
            context = initSslContext();
        } catch (Exception e) {
            throw new WorkflowCertificateException(e);
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    }

    public X509Certificate[] getCertificatesForUri(URI uri) {
        X509Certificate[] chain = getServerCertsForUri(uri);

        // assuming uri was not trusted so that certs were saved by Trust Manager
        if (chain == null) {
            chain = workflowTrustManager.chain;
        }

        if (chain == null) {
            throw new WorkflowCertificateException("Could not obtain server certificate chain for host " + uri.getHost());
        }
        return chain;
    }

    public void saveCertForUri(URI uri) {
        if (!uri.getScheme().equals("https")) {
            logger.info("Uri {} is not https so skipping cert check", uri.toString());
            return;
        }
        if (isUriTrusted(uri)) {
            return;
        }

        X509Certificate[] chain = workflowTrustManager.chain;
        if (chain == null) {
            throw new WorkflowCertificateException("Could not obtain server certificate chain for host " + uri.getHost());
        }

        logger.info("Saving ssl cert for host {} to local keystore file {}", uri.getHost(), keyStoreFile.getPath());
        storeCert(uri.getHost(), chain[0]);
        try {
            context = initSslContext();
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            throw new WorkflowCertificateException(e);
        }
        if (!isUriTrusted(uri)) {
            throw new WorkflowCertificateException("Expected host " + uri.getHost() + " to be trusted after saving cert!");
        }
    }

    public boolean isUriTrusted(URI uri) {
        if (trustedHosts.contains(uri.getHost())) {
            return true;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.getResponseCode();
            connection.disconnect();
            logger.debug("No errors, host {} is trusted", uri.getHost());
            trustedHosts.add(uri.getHost());
            return true;
        } catch (SSLException e) {
            logger.debug("Host {} is not trusted\n{}", uri.getHost(), StringUtils.exceptionAsString(e));
            return false;
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public boolean isLastServerTrusted() {
        return workflowTrustManager.isLastServerTrusted();
    }

    public String getKeyStore() {
        return keyStoreFile.getPath();
    }

    private void storeCert(String host, X509Certificate certificate) {
        String alias = host + "-1";
        try {
            workflowKeystore.setCertificateEntry(alias, certificate);
        } catch (KeyStoreException e) {
            throw new WorkflowCertificateException(e);
        }

        storeKeystore();
    }

    private void storeKeystore() {
        try (OutputStream out = new FileOutputStream(keyStoreFile)) {
            workflowKeystore.store(out, PASS_PHRASE);
            out.close();
        } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new WorkflowCertificateException(e);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private X509KeyManager getKeyManager(String algorithm, KeyStore keystore, char[] password) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        KeyManagerFactory factory = KeyManagerFactory.getInstance(algorithm);
        factory.init(keystore, password);
        return getFirstX509KeyManager(factory.getKeyManagers());
    }

    private X509TrustManager getTrustManager(String algorithm, KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(algorithm);
        factory.init(keystore);
        return getFirstX509TrustManager(factory.getTrustManagers());
    }

    private SSLContext initSslContext() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException, IOException, CertificateException {
        createKeyStore();
        String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        X509KeyManager customKeyManager = getKeyManager("SunX509", workflowKeystore, PASS_PHRASE);
        X509KeyManager jvmKeyManager = getKeyManager(defaultAlgorithm, null, null);
        X509TrustManager customTrustManager = getTrustManager("SunX509", workflowKeystore);
        X509TrustManager jvmTrustManager = getTrustManager(defaultAlgorithm, null);
        workflowTrustManager = new SavingTrustManager(Arrays.asList(jvmTrustManager, customTrustManager));

        KeyManager[] keyManagers = { new CompositeX509KeyManager(Arrays.asList(jvmKeyManager, customKeyManager)) };
        TrustManager[] trustManagers = { workflowTrustManager };

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(keyManagers, trustManagers, null);
        return context;
    }

    private X509KeyManager getFirstX509KeyManager(KeyManager[] keyManagers) {
        return Arrays.stream(keyManagers).filter(keyManager -> keyManager instanceof X509KeyManager)
                .map(keyManager -> (X509KeyManager) keyManager)
                .findFirst().orElseThrow(() -> new WorkflowCertificateException("No key manager found"));
    }

    private X509TrustManager getFirstX509TrustManager(TrustManager[] trustManagers) {
        return Arrays.stream(trustManagers).filter(trustManager -> trustManager instanceof X509TrustManager)
                .map(trustManager -> (X509TrustManager) trustManager)
                .findFirst().orElseThrow(() -> new WorkflowCertificateException("No trust manager found"));
    }

    private void createKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (keyStoreFile == null) {
            logger.debug("No keystore file set");
            return;
        }
        if (!keyStoreFile.exists()) {
            logger.info("Creating blank keystore file {}", keyStoreFile.getPath());
            keyStoreFile.createNewFile();
        }
        workflowKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        if (keyStoreFile.length() == 0) {
            workflowKeystore.load(null, PASS_PHRASE);
        } else {
            try (InputStream fileInput = new FileInputStream(keyStoreFile)) {
                workflowKeystore.load(fileInput, PASS_PHRASE);
            }
        }
    }

    private X509Certificate[] getServerCertsForUri(URI uri) {
        try {
            URLConnection connection = uri.toURL().openConnection();
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).getResponseCode();
                Certificate[] serverCerts = ((HttpsURLConnection) connection).getServerCertificates();
                return Arrays.stream(serverCerts).filter(certificate -> certificate instanceof X509Certificate)
                        .map(certificate -> ((X509Certificate) certificate)).toArray(X509Certificate[]::new);
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static class SavingTrustManager extends CompositeX509TrustManager {
        private X509Certificate[] chain;
        private boolean lastServerTrusted;

        SavingTrustManager(List<X509TrustManager> trustManagers) {
            super(trustManagers);
        }

        public boolean isLastServerTrusted() {
            return lastServerTrusted;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.lastServerTrusted = false;
            this.chain = chain;
            super.checkServerTrusted(chain, authType);
            this.lastServerTrusted = true;
        }
    }

}

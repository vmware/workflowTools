package com.vmware.http.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
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

    public WorkflowCertificateManager(String keyStoreFile) throws IOException {
        this.keyStoreFile = new File(keyStoreFile);
        try {
            context = initSslContext();
        } catch (Exception e) {
            throw new IOException(e);
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    }

    public void saveCertForUri(URI uri) throws IOException {
        if (!uri.getScheme().equals("https")) {
            logger.info("Uri {} is not https so skipping cert check", uri.toString());
            return;
        }
        if (isUriTrusted(uri)) {
            return;
        }

        X509Certificate[] chain = workflowTrustManager.chain;
        if (chain == null) {
            throw new RuntimeException("Could not obtain server certificate chain for host " + uri.getHost());
        }

        logger.info("Saving ssl cert for host {} to local keystore file", uri.getHost(), keyStoreFile.getPath());
        storeCert(uri.getHost(), chain[0]);
        try {
            context = initSslContext();
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (!isUriTrusted(uri)) {
            throw new RuntimeException("Expected host " + uri.getHost() + " to be trusted after saving cert!");
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    }

    public boolean isUriTrusted(URI uri) throws IOException {
        if (trustedHosts.contains(uri.getHost())) {
            return true;
        }
        try {
            SSLSocketFactory sslFactory = context.getSocketFactory();
            int port = uri.getPort() == -1 ? 443 : uri.getPort();
            SSLSocket socket = (SSLSocket) sslFactory.createSocket(uri.getHost(), port);
            socket.setSoTimeout(10000);
            logger.debug("Checking if host {} is trusted", uri.getHost());
            socket.startHandshake();
            socket.close();
            logger.debug("No errors, host {} is trusted", uri.getHost());
            trustedHosts.add(uri.getHost());
            return true;
        } catch (SSLException e) {
            logger.debug("Host " + uri.getHost() + " is not trusted", e);
            return false;
        }
    }

    public String getKeyStore() {
        return keyStoreFile.getPath();
    }

    private void storeCert(String host, X509Certificate certificate) throws IOException {
        String alias = host + "-1";
        try {
            workflowKeystore.setCertificateEntry(alias, certificate);
        } catch (KeyStoreException e) {
            throw new IOException(e);
        }

        storeKeystore();
    }

    private void storeKeystore() throws IOException {
        try (OutputStream out = new FileOutputStream(keyStoreFile)) {
            workflowKeystore.store(out, PASS_PHRASE);
            out.close();
        } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new IOException(e);
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
        workflowTrustManager = new SavingTrustManager(customTrustManager);
        X509TrustManager jvmTrustManager = getTrustManager(defaultAlgorithm, null);

        KeyManager[] keyManagers = { new CompositeX509KeyManager(Arrays.asList(jvmKeyManager, customKeyManager)) };
        TrustManager[] trustManagers = { new CompositeX509TrustManager(Arrays.asList(jvmTrustManager, workflowTrustManager)) };

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(keyManagers, trustManagers, null);
        return context;
    }

    private X509KeyManager getFirstX509KeyManager(KeyManager[] keyManagers) {
        for (KeyManager keyManager : keyManagers) {
            if (keyManager instanceof X509KeyManager) {
                return (X509KeyManager) keyManager;
            }
        }
        throw new RuntimeException("No key manager found");
    }

    private X509TrustManager getFirstX509TrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new RuntimeException("No trust manager found");
    }

    private void createKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
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

    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            return tm.getAcceptedIssuers();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }

}

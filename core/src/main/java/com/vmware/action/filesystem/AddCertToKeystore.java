package com.vmware.action.filesystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

import static com.vmware.config.section.SslConfig.BEGIN_PRIVATE_KEY;
import static com.vmware.config.section.SslConfig.END_PRIVATE_KEY;
import static sun.security.provider.X509Factory.BEGIN_CERT;
import static sun.security.provider.X509Factory.END_CERT;

@ActionDescription("Adds the loaded certificate to the specified keystore.")
public class AddCertToKeystore extends BaseAction {
    public AddCertToKeystore(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("fileData", "keystoreFile", "keystorePassword", "keystoreAlias", "keystoreAliasPassword");
    }

    @Override
    public void process() {
        try {
            File keystoreFile = new File(sslConfig.keystoreFile);
            KeyStore privateKS;
            if (keystoreFile.exists()) {
                privateKS = loadKeyStore(keystoreFile);
            } else {
                failIfEmpty(sslConfig.newKeystoreType, "newKeystoreType not set");
                privateKS = createKeyStore(sslConfig.newKeystoreType);
                log.info("Created keystore {} of type {}", keystoreFile.getAbsolutePath(), sslConfig.newKeystoreType);
            }
;
            X509Certificate certificate = createCertificate();
            KeyPair pair = createKeyPair();
            certificate.verify(pair.getPublic());

            privateKS.setKeyEntry(sslConfig.keystoreAlias, pair.getPrivate(), sslConfig.keystoreAliasPassword.toCharArray(),
                    new Certificate[] { certificate });
            privateKS.store(new FileOutputStream(sslConfig.keystoreFile), sslConfig.keystorePassword.toCharArray());

            log.info("Added certificate with alias {} to keystore {}", sslConfig.keystoreAlias, sslConfig.keystoreFile);

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private KeyStore loadKeyStore(File keystoreFile) {
        List<String> keystoreTypes = new ArrayList<>(Arrays.asList("JCEKS", "PKCS12", "JKS"));
        if (sslConfig.additionalKeystoreTypes != null) {
            keystoreTypes.addAll(sslConfig.additionalKeystoreTypes.keySet());
        }
        return keystoreTypes.stream().map(keystoreType -> {
            try {
                KeyStore privateKS = createKeyStore(keystoreType);
                privateKS.load(new FileInputStream(keystoreFile), sslConfig.keystorePassword.toCharArray());
                log.info("Loaded keystore {} of type {}", keystoreFile.getAbsolutePath(), keystoreType);
                return privateKS;
            } catch (Throwable t) {
                log.debug("Failed with keystore type {}\n{}", keystoreType, StringUtils.exceptionAsString(t));
                return null;
            }
        }).filter(Objects::nonNull).findFirst().orElseThrow(() -> new FatalException("Unable to load keystore file {}",
                keystoreFile.getPath(), sslConfig.keystorePassword));
    }

    private KeyStore createKeyStore(String keystoreType)
            throws MalformedURLException, ClassNotFoundException, KeyStoreException, IllegalAccessException, InstantiationException {
        if (sslConfig.additionalKeystoreTypes == null || !sslConfig.additionalKeystoreTypes.containsKey(keystoreType)) {
            return KeyStore.getInstance(keystoreType);
        }
        List<String> providerInfo = sslConfig.additionalKeystoreTypes.get(keystoreType);

        failIfTrue(providerInfo == null || providerInfo.size() != 2, "Invalid value " + providerInfo + " for keystore " + keystoreType);
        String filePath = replacementVariables.replaceVariablesInValue(providerInfo.get(1));
        File providerJarFile = new File(filePath);
        failIfTrue(!providerJarFile.exists(), "file " + providerJarFile.getAbsolutePath() + " path not set");

        log.debug("Using jar file {} for provider {}", providerJarFile.getAbsolutePath(), providerInfo.get(0));
        URL jarUrl = new URL("jar:file:" + providerJarFile.getAbsolutePath() + "!/");
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] {jarUrl}, getClass().getClassLoader());
        Provider provider = (Provider) classLoader.loadClass(providerInfo.get(0)).newInstance();
        return KeyStore.getInstance(keystoreType, provider);
    }

    private X509Certificate createCertificate() throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        String certOutput = StringUtils.findStringWithStartAndEnd(fileSystemConfig.fileData, BEGIN_CERT, END_CERT);

        X509Certificate certificate = (X509Certificate) CertificateFactory
                .getInstance("X.509").generateCertificate(new ByteArrayInputStream(certOutput.getBytes(StandardCharsets.UTF_8)));
        certificate.checkValidity();
        certificate.verify(certificate.getPublicKey());
        return certificate;
    }

    private KeyPair createKeyPair() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String keyOutput = StringUtils.findStringBetween(fileSystemConfig.fileData, BEGIN_PRIVATE_KEY, END_PRIVATE_KEY).replace("\n","");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyOutput.getBytes(StandardCharsets.UTF_8)));
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) kf.generatePrivate(keySpec);

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());

        PublicKey publicKey = kf.generatePublic(publicKeySpec);
        return new KeyPair(publicKey, privateKey);
    }
}

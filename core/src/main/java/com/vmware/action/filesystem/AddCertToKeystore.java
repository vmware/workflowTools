package com.vmware.action.filesystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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
import java.util.Base64;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import static com.vmware.config.section.SslConfig.BEGIN_PRIVATE_KEY;
import static com.vmware.config.section.SslConfig.END_PRIVATE_KEY;
import static sun.security.provider.X509Factory.BEGIN_CERT;
import static sun.security.provider.X509Factory.END_CERT;

@ActionDescription("Adds the loaded certificate to the specified keystore.")
public class AddCertToKeystore extends BaseAction {
    public AddCertToKeystore(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("fileData", "keystoreFile", "keystorePassword", "keystoreType", "keystoreAlias", "keystoreAliasPassword");
    }

    @Override
    public void process() {
        try {
            KeyStore privateKS = KeyStore.getInstance(sslConfig.keystoreType);
            File keystoreFile = new File(sslConfig.keystoreFile);
            if (keystoreFile.exists()) {
                privateKS.load(new FileInputStream(sslConfig.keystoreFile), sslConfig.keystorePassword.toCharArray());
            } else {
                privateKS.load(null);
            }
;
            X509Certificate certificate = createCertificate();
            KeyPair pair = createKeyPair();
            certificate.verify(pair.getPublic());


            privateKS.setKeyEntry(sslConfig.keystoreAlias, pair.getPrivate(), sslConfig.keystoreAliasPassword.toCharArray(),
                    new Certificate[] { certificate });
            privateKS.store(new FileOutputStream(sslConfig.keystoreFile), sslConfig.keystorePassword.toCharArray());

            log.info("Added certificate with alias {} to keystore {}", sslConfig.keystoreAlias, sslConfig.keystoreFile);

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | InvalidKeyException | NoSuchProviderException | SignatureException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
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

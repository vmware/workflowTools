package com.vmware.action.filesystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
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
import java.util.Calendar;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.vmware.action.base.BaseSslAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;

import static com.vmware.util.StringUtils.*;

@ActionDescription(value = "Saves the loaded cert. If keystore password is set, cert is saved to a keystore, otherwise as pem files.",
        configFlagsToAlwaysExcludeFromCompleter = {"--cipher-salt-length", "--new-keystore-type"})
public class SaveCert extends BaseSslAction {
    public SaveCert(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("fileData", "destinationFile", "keystoreAliasPassword");
    }

    @Override
    public void process() {
        try {
            if (StringUtils.isEmpty(sslConfig.keystorePassword)) {
                saveCertToPemFile();
            } else {
                addCertToKeyStoreFile();
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveCertToPemFile()
            throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidKeySpecException {
        log.info("No keystore password set, assuming keystore file {} corresponds to key file and pem file pairing", fileSystemConfig.destinationFile);

        X509Certificate certificate = createCertificate();
        KeyPair pair = createKeyPair();
        certificate.verify(pair.getPublic());

        File keyFile = new File(fileSystemConfig.destinationFile + ".key");
        File certificateFile = new File(fileSystemConfig.destinationFile + ".pem");

        String keyPem;
        if (StringUtils.isNotBlank(sslConfig.keystoreAliasPassword)) {
            keyPem = encrypt(pair.getPrivate(), sslConfig.keystoreAliasPassword.toCharArray());
            log.info("Saving encrypted private key to {}", keyFile.getAbsolutePath());
        } else {
            keyPem = StringUtils.convertToPem(pair.getPrivate());
            log.info("Saving unencrypted private key to {}", keyFile.getAbsolutePath());
        }

        IOUtils.write(keyFile, keyPem);

        log.info("Saving certificate to {}", certificateFile.getAbsolutePath());
        IOUtils.write(certificateFile, StringUtils.convertToPem(certificate));
    }

    private void addCertToKeyStoreFile()
            throws ClassNotFoundException, KeyStoreException, IllegalAccessException, InstantiationException, CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException, SignatureException, InvalidKeySpecException, IOException {
        failIfEmpty("keystoreAliasPassword", "keystoreAliasPassword not set");
        failIfEmpty("keystoreAlias", "keystoreAlias not set");
        File keystoreFile = new File(fileSystemConfig.destinationFile);
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
        privateKS.store(new FileOutputStream(fileSystemConfig.destinationFile), sslConfig.keystorePassword.toCharArray());

        log.info("Added certificate with alias {} to keystore {}", sslConfig.keystoreAlias, fileSystemConfig.destinationFile);
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

    private String encrypt(Key key, char[] password) {
        try {
            String algorithm = sslConfig.cipherForPrivateKey;

            SecretKeyFactory keyFact = SecretKeyFactory.getInstance(algorithm);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
            SecretKey pbeKey = keyFact.generateSecret(pbeKeySpec);

            byte[] salt = randomBytes(sslConfig.cipherSaltLength);
            int iterationCount = generateIterationCount();
            PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(salt, iterationCount);
            AlgorithmParameters params = AlgorithmParameters.getInstance(algorithm);
            params.init(pbeParameterSpec);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.WRAP_MODE, pbeKey, params);

            byte[] encryptedBytes = cipher.wrap(key);
            EncryptedPrivateKeyInfo privateKeyInfo = new EncryptedPrivateKeyInfo(params, encryptedBytes);

            return StringUtils.convertToPem(privateKeyInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int generateIterationCount() {
        Random rng = new Random();
        rng.setSeed(Calendar.getInstance().getTimeInMillis());

        int random = rng.nextInt();
        int mod1000 = random % 1000;
        return mod1000 + 1000;
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

}

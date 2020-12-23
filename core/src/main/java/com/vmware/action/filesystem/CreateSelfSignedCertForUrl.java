package com.vmware.action.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Base64;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.FileUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.LogLevel;

import static com.vmware.config.section.SslConfig.BEGIN_PRIVATE_KEY;
import static com.vmware.config.section.SslConfig.END_PRIVATE_KEY;
import static sun.security.provider.X509Factory.BEGIN_CERT;
import static sun.security.provider.X509Factory.END_CERT;

@ActionDescription("Created a self signed cert. The host of the specified url is used as the CN value.")
public class CreateSelfSignedCertForUrl extends BaseAction {

    public final static String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String IP_ADDRESS_REGEX = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";

    public CreateSelfSignedCertForUrl(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceUrl");
        super.addExpectedCommandsToBeAvailable("keytool");
    }

    @Override
    public void process() {
        log.info("Creating self signed cert for url {}", fileSystemConfig.sourceUrl);
        URI sourceUri = URI.create(fileSystemConfig.sourceUrl);
        File tempKeystoreFile = FileUtils.createTempFile("keystore", ".ks");
        tempKeystoreFile.delete();
        String command = "keytool -genkey -keystore " + tempKeystoreFile.getAbsolutePath()
                + " -keyalg RSA -keysize " + sslConfig.keySize +
                " -validity 365 -alias selfsign -dname \"cn=" + sourceUri.getHost() + "\" -storepass password -keypass password";
        try {
            InetAddress inetAddress = InetAddress.getByName(sourceUri.getHost());
            if (inetAddress.getHostAddress() != null && inetAddress.getHostAddress().equals(sourceUri.getHost())) {
                log.info("Adding subject alternative name for ip address " + sourceUri.getHost());
                command += " -ext san=ip:" + sourceUri.getHost();
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        String keytoolOutput = CommandLineUtils.executeCommand(command, LogLevel.DEBUG);
        tempKeystoreFile.deleteOnExit();

        try {
            KeyStore keystoreForCreatedCert = KeyStore.getInstance(sslConfig.keystoreType);
            keystoreForCreatedCert.load(new FileInputStream(tempKeystoreFile), "password".toCharArray());

            KeyStore.PrivateKeyEntry entry =
                    (KeyStore.PrivateKeyEntry) keystoreForCreatedCert.getEntry("selfsign", new KeyStore.PasswordProtection("password".toCharArray()));

            String privateKeyPem = formatKeyContents(entry.getPrivateKey());
            String certPem = formatCrtFileContents(entry.getCertificate());

            fileSystemConfig.fileData = privateKeyPem + LINE_SEPARATOR + certPem ;
            log.info("Self signed cert:\n{}", fileSystemConfig.fileData);
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | UnrecoverableEntryException e) {
            throw new FatalException(e, "Failed to convert certificate pem to X509 Certificate\n" + keytoolOutput);
        }
    }

    public static String formatCrtFileContents(final Certificate certificate) throws CertificateEncodingException {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());

        final byte[] rawCrtText = certificate.getEncoded();
        final String encodedCertText = new String(encoder.encode(rawCrtText));
        return BEGIN_CERT + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_CERT;
    }

    public static String formatKeyContents(final PrivateKey privateCrtKey) throws CertificateEncodingException {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());

        final byte[] rawCrtText = privateCrtKey.getEncoded();
        final String encodedCertText = new String(encoder.encode(rawCrtText));
        return BEGIN_PRIVATE_KEY + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_PRIVATE_KEY;
    }
}

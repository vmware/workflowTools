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
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.FileUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.LogLevel;

import static com.vmware.util.StringUtils.convertToPem;
import static com.vmware.util.StringUtils.convertToPem;

@ActionDescription(value = "Created a self signed cert. The host of the specified url is used as the CN value.",
        configFlagsToExcludeFromCompleter = "--keystore-type")
public class CreateSelfSignedCertForUrl extends BaseAction {

    public final static String LINE_SEPARATOR = System.getProperty("line.separator");

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
                + " -storetype PKCS12 -keyalg RSA -keysize " + sslConfig.keySize +
                " -validity 365 -ext KeyUsage=digitalSignature,keyEncipherment,keyCertSign -alias selfsign -dname \"cn=" + sourceUri.getHost() + "\" -storepass password -keypass password";
        try {
            InetAddress inetAddress = InetAddress.getByName(sourceUri.getHost());
            if (inetAddress.getHostAddress() != null && !inetAddress.getHostAddress().equals(sourceUri.getHost())) {
                log.info("Adding subject alternative name {} for host name {}", inetAddress.getHostAddress(), sourceUri.getHost());
                command += " -ext san=ip:" + inetAddress.getHostAddress();
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        String keytoolOutput = CommandLineUtils.executeCommand(command, LogLevel.DEBUG);
        tempKeystoreFile.deleteOnExit();

        try {
            KeyStore keystoreForCreatedCert = KeyStore.getInstance("PKCS12");
            keystoreForCreatedCert.load(new FileInputStream(tempKeystoreFile), "password".toCharArray());

            KeyStore.PrivateKeyEntry entry =
                    (KeyStore.PrivateKeyEntry) keystoreForCreatedCert.getEntry("selfsign", new KeyStore.PasswordProtection("password".toCharArray()));

            String privateKeyPem = StringUtils.convertToPem(entry.getPrivateKey());
            String certPem = convertToPem(entry.getCertificate());

            fileSystemConfig.fileData = privateKeyPem + LINE_SEPARATOR + certPem ;
            log.info("Self signed cert:\n{}", fileSystemConfig.fileData);
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | UnrecoverableEntryException e) {
            throw new FatalException(e, "Failed to convert certificate pem to X509 Certificate\n" + keytoolOutput);
        }
    }
}

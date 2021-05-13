package com.vmware.action.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseSslAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

import static com.vmware.util.StringUtils.LINE_SEPARATOR;

@ActionDescription("Loads a certificate from a keystore")
public class LoadCertFromKeystore extends BaseSslAction {
    public LoadCertFromKeystore(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile", "keystoreAlias", "keystoreAliasPassword", "keystorePassword");
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        super.failIfTrue(!new File(fileSystemConfig.sourceFile).exists(), "Source file " + fileSystemConfig.sourceFile + " does not exist");
    }

    @Override
    public void process() {
        try {
            log.info("Loading alias {} from keystore {}", sslConfig.keystoreAlias, fileSystemConfig.sourceFile);
            KeyStore keystore = loadKeyStore(new File(fileSystemConfig.sourceFile));
            Key privateKey = keystore.getKey(sslConfig.keystoreAlias, sslConfig.keystoreAliasPassword.toCharArray());
            Certificate cert = keystore.getCertificate(sslConfig.keystoreAlias);

            fileSystemConfig.fileData = StringUtils.convertToPem(privateKey) + LINE_SEPARATOR + StringUtils.convertToPem(cert);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}

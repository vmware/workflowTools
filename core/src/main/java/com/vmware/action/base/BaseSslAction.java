package com.vmware.action.base;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

public abstract class BaseSslAction extends BaseAction {
    public BaseSslAction(WorkflowConfig config) {
        super(config);
    }

    protected KeyStore loadKeyStore(File keystoreFile) {
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

    protected KeyStore createKeyStore(String keystoreType)
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
}

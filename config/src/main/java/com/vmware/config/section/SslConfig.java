package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class SslConfig {
    public static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    public static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    @ConfigurableProperty(commandLine = "--cipher-key", help = "Base64 Encoded cipher key")
    public String cipherKey;

    @ConfigurableProperty(commandLine = "--keystore-type", help = "Keystore type")
    public String keystoreType;

    @ConfigurableProperty(commandLine = "--keystore-file", help = "Keystore file to use")
    public String keystoreFile;

    @ConfigurableProperty(commandLine = "--keystore-password", help = "Keystore password to use")
    public String keystorePassword;

    @ConfigurableProperty(commandLine = "--keystore-alias", help = "Alias for certificate in the keystore")
    public String keystoreAlias;

    @ConfigurableProperty(commandLine = "--keystore-alias-password", help = "Keystore password to use")
    public String keystoreAliasPassword;

    @ConfigurableProperty(commandLine = "--key-size", help = "Key size for entry in the keystore")
    public int keySize;
}

package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class SslConfig {
    public static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    public static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    @ConfigurableProperty(commandLine = "--keystore-type", help = "Keystore type")
    public String keystoreType;

    @ConfigurableProperty(help = "Key size for entry in the keystore")
    public int keySize;

    @ConfigurableProperty(commandLine = "--cipher-key", help = "Base64 Encoded cipher key")
    public String cipherKey;

    @ConfigurableProperty(commandLine = "--keystore-file", help = "Keystore file to use")
    public String keystoreFile;

    @ConfigurableProperty(commandLine = "--keystore-password", help = "Keystore password to use")
    public String keystorePassword;

    @ConfigurableProperty(commandLine = "--keystore-alias", help = "Alias for certificate in the keystore")
    public String keystoreAlias;

    @ConfigurableProperty(commandLine = "--keystore-alias-password", help = "Keystore password to use")
    public String keystoreAliasPassword;

    @ConfigurableProperty(help = "Key algorithm for unwrapping or decrypting a value")
    public String cipherKeyAlgorithm;

    @ConfigurableProperty(commandLine = "--cipher-transformation", help = "Cipher transformation to use")
    public String cipherTransformation;

    @ConfigurableProperty(help = "Cipher salt length for decrypting a value")
    public int cipherSaltLength;
}

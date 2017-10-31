package com.vmware.http.ssl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.X509KeyManager;

/**
 * Represents an ordered list of {@link X509KeyManager}s with most-preferred managers first.
 *
 * This is necessary because of the fine-print on {SSLContext#init}:
 *     Only the first instance of a particular key and/or trust manager implementation type in the
 *     array is used. (For example, only the first javax.net.ssl.X509KeyManager in the array will be used.)
 *
 * @author codyaray
 * @since 4/22/2013
 */
public class CompositeX509KeyManager implements X509KeyManager {

    private final List<X509KeyManager> keyManagers;

    /**
     * Creates a new {@link CompositeX509KeyManager}.
     *
     * @param keyManagers the X509 key managers, ordered with the most-preferred managers first.
     */
    public CompositeX509KeyManager(List<X509KeyManager> keyManagers) {
        this.keyManagers = new ArrayList<>(keyManagers);
    }

    /**
     * Chooses the first non-null client alias returned from the delegate
     * {X509TrustManagers}, or {@code null} if there are no matches.
     */
    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return keyManagers.stream().map(keyManager -> keyManager.chooseClientAlias(keyType, issuers, socket))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Chooses the first non-null server alias returned from the delegate
     * {X509TrustManagers}, or {@code null} if there are no matches.
     */
    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return keyManagers.stream().map(keyManager -> keyManager.chooseServerAlias(keyType, issuers, socket))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Returns the first non-null private key associated with the
     * given alias, or {@code null} if the alias can't be found.
     */
    @Override
    public PrivateKey getPrivateKey(String alias) {
        return keyManagers.stream().map(keyManager -> keyManager.getPrivateKey(alias))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Returns the first non-null certificate chain associated with the
     * given alias, or {@code null} if the alias can't be found.
     */
    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return keyManagers.stream().map(keyManager -> keyManager.getCertificateChain(alias))
                .filter(chain -> chain != null && chain.length > 0).findFirst().orElse(null);
    }

    /**
     * Get all matching aliases for authenticating the client side of a
     * secure socket, or {@code null} if there are no matches.
     */
    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        List<String> aliases = new ArrayList<>();
        for (X509KeyManager keyManager : keyManagers) {
            aliases.addAll(Arrays.asList(keyManager.getClientAliases(keyType, issuers)));
        }
        return emptyToNull(aliases.toArray(new String[aliases.size()]));
    }

    /**
     * Get all matching aliases for authenticating the server side of a
     * secure socket, or {@code null} if there are no matches.
     */
    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        List<String> aliases = new ArrayList<>();
        for (X509KeyManager keyManager : keyManagers) {
            aliases.addAll(Arrays.asList(keyManager.getServerAliases(keyType, issuers)));
        }
        return emptyToNull(aliases.toArray(new String[aliases.size()]));
    }

    private static <T> T[] emptyToNull(T[] arr) {
        return (arr.length == 0) ? null : arr;
    }

}
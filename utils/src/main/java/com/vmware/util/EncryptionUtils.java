package com.vmware.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class EncryptionUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 8;
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    public static byte[] decrypt(String data, String key) {
        try {
            byte[] cipherData = DatatypeConverter.parseBase64Binary(data);
            byte[] keyData = DatatypeConverter.parseBase64Binary(key);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            SecretKeySpec skeySpec = new SecretKeySpec(keyData, KEY_ALGORITHM);
            IvParameterSpec paramSpec = new IvParameterSpec(extractIv(cipherData, keyData.length));
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, paramSpec);
            byte[] decryptedData = cipher.doFinal(extractCipherData(cipherData, keyData.length));
            byte[] plainData = new byte[decryptedData.length - SALT_LENGTH];
            System.arraycopy(decryptedData, SALT_LENGTH, plainData, 0, plainData.length);
            return plainData;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

    }

    public static byte[] encrypt(byte[] data, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            SecretKeySpec skeySpec = new SecretKeySpec(key, KEY_ALGORITHM);
            byte [] iv = new byte[key.length];
            SECURE_RANDOM.nextBytes(iv);
            AlgorithmParameterSpec algorithmParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, algorithmParameterSpec);
            byte[] saltedData = prependSalt(data);
            return prependIv(cipher.doFinal(saltedData),iv);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] extractIv(byte[] data, int keyLength) {
        if (data.length <= keyLength) {
            throw new ArrayIndexOutOfBoundsException(data.length);
        }
        byte[] result = new byte[keyLength];
        System.arraycopy(data, 0, result, 0, keyLength);
        return result;
    }

    private static byte[] extractCipherData(byte[] data, int keyLength) {
        if (data.length <= keyLength) {
            throw new ArrayIndexOutOfBoundsException(data.length);
        }
        byte[] result = new byte[data.length - keyLength];
        System.arraycopy(data, keyLength, result, 0, result.length);
        return result;
    }

    private static byte[] prependIv(byte[] data, byte [] iv) {
        byte[] result = new byte[data.length+iv.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(data, 0, result, iv.length, data.length);
        return result;
    }

    private static byte[] prependSalt(byte[] plainData) {
        byte[] salt = new byte[SALT_LENGTH];
        // Using secure random number generator rather than the default
        // Random class.
        SECURE_RANDOM.nextBytes(salt);
        byte[] saltedData = new byte[plainData.length + SALT_LENGTH];
        System.arraycopy(salt, 0, saltedData, 0, SALT_LENGTH);
        System.arraycopy(plainData, 0, saltedData, SALT_LENGTH, plainData.length);
        return saltedData;
    }

    public static void main(String[] args) {
        String password = new String(decrypt("7wrxsoHcntkUHQDRQ6f8ZQ==", "Zx5BEHvyy0ko2l4yZ68iHO1PMdhERiY4xcTbG95Mtm0="));
        System.out.println(password);
    }
}

package com.vmware.action.filesystem;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.SkipActionException;

@ActionDescription("Decrypts the specified property to the specified variable name.")
public class DecryptVariable extends BaseAction {

    public DecryptVariable(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("cipherKey", "cipherDecryptTransformation", "cipherSaltLength", "cipherKeyAlgorithm",
                "propertyValue", "outputVariableName");
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        try {
            super.checkIfActionShouldBeSkipped();
        } catch (SkipActionException sae) {
            if (StringUtils.isNotBlank(fileSystemConfig.outputVariableName)) {
                replacementVariables.addVariable(fileSystemConfig.outputVariableName, "", false);
            }
            throw sae;
        }
    }

    @Override
    public void process() {
        try {
            byte[] realKey = Base64.getDecoder().decode(sslConfig.cipherKey);

            String decryptedValue = decryptValue(realKey, fileSystemConfig.propertyValue);
            log.debug("Decrypted value {} for value {}", decryptedValue, fileSystemConfig.propertyValue);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, decryptedValue, false);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, "", false);
            throw new SkipActionException("failed to decrypt value {}. Check if the cipher key is correct."
                    + "\nReason: {}", fileSystemConfig.propertyValue, e.getMessage());
        }
    }

    private String decryptValue(byte[] cryptoKey, String value) throws GeneralSecurityException {
        byte[] valueAsByteArray = Base64.getDecoder().decode(value);
        Cipher cipher = Cipher.getInstance(sslConfig.cipherDecryptTransformation);
        SecretKeySpec skeySpec = new SecretKeySpec(cryptoKey, sslConfig.cipherKeyAlgorithm);
        IvParameterSpec paramSpec = new IvParameterSpec(extractIv(cryptoKey.length, valueAsByteArray));
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, paramSpec);
        byte[] decryptedData = cipher.doFinal(extractCipherData(cryptoKey.length, valueAsByteArray));
        byte[] plainData = new byte[decryptedData.length - sslConfig.cipherSaltLength];
        System.arraycopy(decryptedData, sslConfig.cipherSaltLength, plainData, 0, plainData.length);
        return new String(plainData, StandardCharsets.UTF_8);
    }

    private byte[] extractIv(int keySize, byte[] data) {
        if (data.length <= keySize) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte[] result = new byte[keySize];
        System.arraycopy(data, 0, result, 0, keySize);
        return result;
    }

    private byte[] extractCipherData(int keySize, byte[] data) {
        if (data.length <= keySize) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte[] result = new byte[data.length - keySize];
        System.arraycopy(data, keySize, result, 0, result.length);
        return result;
    }
}

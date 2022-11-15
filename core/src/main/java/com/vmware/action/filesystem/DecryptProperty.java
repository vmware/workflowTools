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

import static com.vmware.util.ArrayUtils.subSection;

@ActionDescription(value = "Decrypts the specified property to the specified variable name.")
public class DecryptProperty extends BaseAction {

    public DecryptProperty(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("cipherKey", "cipherAlgorithm", "cipherSaltLength", "cipherKeyAlgorithm",
                "propertyValue", "outputVariableName");
    }

    @Override
    public void process() {
        try {
            byte[] realKey = Base64.getDecoder().decode(sslConfig.cipherKey);

            String decryptedValue = decryptValue(realKey, fileSystemConfig.propertyValue);
            log.debug("Decrypted value {} for value {}", decryptedValue, fileSystemConfig.propertyValue);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, decryptedValue);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new SkipActionException("failed to decrypt value {}. Check if the cipher key is correct.\n{}",
                    fileSystemConfig.propertyValue, StringUtils.exceptionAsString(e));
        }
    }

    private String decryptValue(byte[] cryptoKey, String value) throws GeneralSecurityException {
        byte[] valueAsByteArray = Base64.getDecoder().decode(value);
        Cipher cipher = Cipher.getInstance(sslConfig.cipherAlgorithm);
        SecretKeySpec skeySpec = new SecretKeySpec(cryptoKey, sslConfig.cipherKeyAlgorithm);

        IvParameterSpec paramSpec = new IvParameterSpec(subSection(valueAsByteArray, 0, cryptoKey.length));
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, paramSpec);
        byte[] decryptedData = cipher.doFinal(subSection(valueAsByteArray, cryptoKey.length, valueAsByteArray.length - cryptoKey.length));

        byte[] plainData = new byte[decryptedData.length - sslConfig.cipherSaltLength];
        System.arraycopy(decryptedData, sslConfig.cipherSaltLength, plainData, 0, plainData.length);
        return new String(plainData, StandardCharsets.UTF_8);
    }
}

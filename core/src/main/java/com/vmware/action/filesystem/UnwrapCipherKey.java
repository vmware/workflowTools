package com.vmware.action.filesystem;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.SkipActionException;

@ActionDescription("Unwraps the specified property using the specified cipher key to the specified variable.")
public class UnwrapCipherKey extends BaseAction {

    public UnwrapCipherKey(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("cipherKey", "cipherAlgorithm", "cipherKeyAlgorithm", "propertyValue", "outputVariableName");
    }

    @Override
    public void process() {
        try {
            byte[] cipherKey = Base64.getDecoder().decode(sslConfig.cipherKey);
            byte[] realKey = unwrap(cipherKey, fileSystemConfig.propertyValue);
            String encodedRealKey = Base64.getEncoder().encodeToString(realKey);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, encodedRealKey);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new SkipActionException("failed to unwrap value {}. Check if the cipher key is correct.\n{}",
                    fileSystemConfig.propertyValue, StringUtils.exceptionAsString(e));
        }
    }

    private byte[] unwrap(byte[] cryptoKey, String value) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        byte[] cipherData = Base64.getDecoder().decode(value);
        Cipher cipher = Cipher.getInstance(sslConfig.cipherAlgorithm);
        SecretKeySpec skeySpec = new SecretKeySpec(cryptoKey, sslConfig.cipherKeyAlgorithm);
        cipher.init(Cipher.UNWRAP_MODE, skeySpec);
        Key decryptedKey = cipher.unwrap(cipherData, sslConfig.cipherKeyAlgorithm, Cipher.SECRET_KEY);
        return decryptedKey.getEncoded();
    }
}

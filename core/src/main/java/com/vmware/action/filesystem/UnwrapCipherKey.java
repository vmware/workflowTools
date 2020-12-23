package com.vmware.action.filesystem;

import java.security.InvalidKeyException;
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
        super.addSkipActionIfBlankProperties("cipherKey", "cipherUnwrapTransformation", "cipherKeyAlgorithm", "propertyValue", "outputVariableName");
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
            byte[] cipherKey = Base64.getDecoder().decode(sslConfig.cipherKey);
            byte[] realKey = unwrap(cipherKey, fileSystemConfig.propertyValue);
            String encodedRealKey = Base64.getEncoder().encodeToString(realKey);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, encodedRealKey, false);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, "", false);
            throw new SkipActionException("failed to unwrap value {}. Check if the cipher key is correct."
                    + "\nReason: {}", fileSystemConfig.propertyValue, e.getMessage());
        }
    }

    private byte[] unwrap(byte[] cryptoKey, String value) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        byte[] cipherData = Base64.getDecoder().decode(value);
        Cipher cipher = Cipher.getInstance(sslConfig.cipherUnwrapTransformation);
        SecretKeySpec skeySpec = new SecretKeySpec(cryptoKey, sslConfig.cipherKeyAlgorithm);
        cipher.init(Cipher.UNWRAP_MODE, skeySpec);
        return cipher.unwrap(cipherData, sslConfig.cipherKeyAlgorithm, Cipher.SECRET_KEY).getEncoded();
    }
}

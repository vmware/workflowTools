package com.vmware.action.filesystem;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.SkipActionException;

import static com.vmware.util.ArrayUtils.join;

@ActionDescription("Encrypts the specified property to the specified variable name.")
public class EncryptProperty extends BaseAction {

    public EncryptProperty(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("cipherKey", "cipherAlgorithm", "cipherSaltLength", "cipherKeyAlgorithm",
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

            String encryptedValue = encrypt(realKey, fileSystemConfig.propertyValue);
            log.debug("Encrypted value {} for value {}", encryptedValue, fileSystemConfig.propertyValue);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, encryptedValue, false);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, "", false);
            throw new SkipActionException("failed to encrypt value {}\n{}", fileSystemConfig.propertyValue, StringUtils.exceptionAsString(e));
        }
    }

    private String encrypt(byte[] cryptoKey, String value) throws GeneralSecurityException {
        byte[] valueAsByteArray = join(randomBytes(sslConfig.cipherSaltLength), value.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(sslConfig.cipherAlgorithm);
        SecretKeySpec skeySpec = new SecretKeySpec(cryptoKey, sslConfig.cipherKeyAlgorithm);
        byte[] iv = randomBytes(cryptoKey.length);
        IvParameterSpec paramSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, paramSpec);

        byte[] encryptedData = cipher.doFinal(valueAsByteArray);
        byte[] ivAndEncryptedData = join(iv, encryptedData);
        return Base64.getEncoder().encodeToString(ivAndEncryptedData);
    }

    private byte[] randomBytes(int size) {
        SecureRandom random = new SecureRandom();
        byte[] value = new byte[size];
        random.nextBytes(value);
        return value;
    }
}

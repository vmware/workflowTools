package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

import java.io.File;

@ActionDescription("Used to manually save api token value for the specified api")
public class SaveApiToken extends BaseAction {
    public SaveApiToken(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("propertyName");
    }

    @Override
    public void process() {
        ApiAuthentication apiAuthentication = ApiAuthentication.loadByName(fileSystemConfig.propertyName);
        if (StringUtils.isEmpty(apiAuthentication.getFileName())) {
            throw new FatalException("Token value cannot be saved for {}", apiAuthentication.name());
        }
        if (StringUtils.isNotBlank(fileSystemConfig.inputText)) {
            log.info(fileSystemConfig.inputText);
        }
        String tokenValue = InputUtils.readValueUntilNotBlank("Enter " + apiAuthentication.name() + " value: ");
        if (apiAuthentication == ApiAuthentication.vcd_refresh) {
            serviceLocator.getVcd().saveRefreshToken(tokenValue); // account for org name
        } else {
            File tokenFile = new File(System.getProperty("user.home") + File.separator + apiAuthentication.getFileName());
            log.info("Saving api token {} to {}", apiAuthentication.name(), tokenFile.getPath());
            IOUtils.write(tokenFile, tokenValue);
        }
    }
}

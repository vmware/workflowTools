package com.vmware.action.vcd;

import java.util.Map;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.vcd.Vcd;

@ActionDescription("Updates the public certificates for the specified url. Reads certs to use from fileData.")
public class UpdateVcdPublicCerts extends BaseAction {
    public UpdateVcdPublicCerts(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("fileData", "sourceUrl", "vcdApiVersion", "vcdAdminUsername", "vcdAdminUserPassword");
    }

    @Override
    public void process() {
        Vcd vcdClientForSystemOrg = new Vcd(fileSystemConfig.sourceUrl, vcdConfig.vcdApiVersion, vcdConfig.vcdAdminUsername, vcdConfig.vcdAdminUserPassword, "System");
        final String resourceType = "application/vnd.vmware.admin.generalsettings";
        Map generalSettings = vcdClientForSystemOrg
                .getResourceAsMap("admin/extension/settings/general", resourceType);
        log.info("Updating public endpoint cerificates with certificate:\n{}\n", fileSystemConfig.fileData);


        generalSettings.compute("restApiBaseUri",
                (k,v) -> v == null || StringUtils.isEmpty(String.valueOf(v)) ? fileSystemConfig.sourceUrl : v);
        generalSettings.compute("tenantPortalExternalAddress",
                (k,v) -> v == null || StringUtils.isEmpty(String.valueOf(v)) ? fileSystemConfig.sourceUrl : v);

        updateCertValueForProperty(generalSettings, "systemExternalAddressPublicCertChain");
        updateCertValueForProperty(generalSettings, "restApiBaseUriPublicCertChain");
        updateCertValueForProperty(generalSettings, "tenantPortalPublicCertChain");

        Map updatedSettings = vcdClientForSystemOrg.updateResourceFromMap("admin/extension/settings/general", generalSettings, resourceType, resourceType);
        checkCertValueMatches(updatedSettings, "systemExternalAddressPublicCertChain");
        checkCertValueMatches(updatedSettings, "restApiBaseUriPublicCertChain");
        checkCertValueMatches(updatedSettings, "tenantPortalPublicCertChain");
        log.info("Successfully updated public certificates for vcd url {}", fileSystemConfig.sourceUrl);
    }

    private void updateCertValueForProperty(Map generalSettings, String key) {
        if (!generalSettings.containsKey(key)) {
            throw new FatalException("Failed to find property {} in general settings {}", key, generalSettings.keySet());
        }
        String existingValue = String.valueOf(generalSettings.put(key, fileSystemConfig.fileData));
        log.info("Replacing existing cert value for {}\n{}\n", key, existingValue);
    }

    private void checkCertValueMatches(Map generalSettings, String key) {
        if (!generalSettings.containsKey(key)) {
            throw new FatalException("Failed to find property {} in general settings {}", key, generalSettings.keySet());
        }
        String existingValue = String.valueOf(generalSettings.get(key));
        if (!existingValue.equals(fileSystemConfig.fileData)) {
            throw new FatalException("Updated cert for {} did not match. Updated value\n{}", key, existingValue);
        }
    }
}

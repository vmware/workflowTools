package com.vmware.action.vcd;

import java.util.Map;

import com.vmware.action.base.BaseFileSystemAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.exception.FatalException;
import com.vmware.vcd.Vcd;

@ActionDescription("Updates the public certificates for the specified vcd url. Reads certs to use from fileData.")
public class UpdatePublicCerts extends BaseFileSystemAction {
    public UpdatePublicCerts(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("vcdUrl", "vcdApiVersion", "vcdSysAdminUser", "vcdSysAdminPassword");
    }

    @Override
    public void process() {

        Vcd vcdClientForSystemOrg = new Vcd(vcdConfig.vcdUrl, vcdConfig.vcdApiVersion, vcdConfig.vcdSysAdminUser, vcdConfig.vcdSysAdminPassword, "System");
        final String resourceType = "application/vnd.vmware.admin.generalsettings";
        Map generalSettings = vcdClientForSystemOrg
                .getResourceAsMap("admin/extension/settings/general", resourceType);
        log.info("Updating public endpoint cerificates with certificate:\n{}\n", fileData);
        updateCertValueForProperty(generalSettings, "systemExternalAddressPublicCertChain");
        updateCertValueForProperty(generalSettings, "restApiBaseUriPublicCertChain");
        updateCertValueForProperty(generalSettings, "tenantPortalPublicCertChain");

        Map updatedSettings = vcdClientForSystemOrg.updateResourceFromMap("admin/extension/settings/general", generalSettings, resourceType, resourceType);
        checkCertValueMatches(updatedSettings, "systemExternalAddressPublicCertChain");
        checkCertValueMatches(updatedSettings, "restApiBaseUriPublicCertChain");
        checkCertValueMatches(updatedSettings, "tenantPortalPublicCertChain");
        log.info("Successfully updated public certificates for vcd url {}", vcdConfig.vcdUrl);
    }

    private void updateCertValueForProperty(Map generalSettings, String key) {
        if (!generalSettings.containsKey(key)) {
            throw new FatalException("Failed to find property {} in general settings {}", key, generalSettings.keySet());
        }
        String existingValue = String.valueOf(generalSettings.put(key, fileData));
        log.info("Replacing existing cert value for {}\n{}\n", key, existingValue);
    }

    private void checkCertValueMatches(Map generalSettings, String key) {
        if (!generalSettings.containsKey(key)) {
            throw new FatalException("Failed to find property {} in general settings {}", key, generalSettings.keySet());
        }
        String existingValue = String.valueOf(generalSettings.get(key));
        if (!existingValue.equals(fileData)) {
            throw new FatalException("Updated cert for {} did not match. Updated value\n{}", key, existingValue);
        }
    }
}

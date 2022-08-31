package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.chrome.ChromeDevTools;
import com.vmware.chrome.domain.ApiRequest;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.UrlUtils;
import com.vmware.vcd.domain.Sites;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ActionDescription("Opens the selected VCenter managed object browser page")
public class OpenVCenterMob extends BaseSingleVappJsonAction {
    public OpenVCenterMob(WorkflowConfig config) {
        super(config);
        super.checkIfSiteSelected = true;
    }

    @Override
    public void process() {
        Sites.DeployedVM selectedVCenter = selectDeployedVm(vappData.getSelectedSite().vcVms(), "VC Server");
        String mobUrl = UrlUtils.addRelativePaths(selectedVCenter.endPointURI, "mob");
        if (fileSystemConfig.autoLogin) {
            log.info("Opening url {} with chrome and auto logging in", mobUrl);
            ChromeDevTools devTools = ChromeDevTools.devTools(fileSystemConfig.chromePath, false, fileSystemConfig.chromeDebugPort);

            Sites.Credentials credentials = selectedVCenter.getLoginCredentials();
            devTools.sendMessage(new ApiRequest("Network.enable"));
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString((credentials.username + ":" + credentials.password).getBytes(StandardCharsets.UTF_8));
            Map<String, String> headers = Collections.singletonMap("Authorization", basicAuth);
            devTools.sendMessage(new ApiRequest("Network.setExtraHTTPHeaders", Collections.singletonMap("headers", headers)));

            devTools.sendMessage(new ApiRequest("Page.enable"));
            devTools.sendMessage(ApiRequest.navigate(mobUrl));
            devTools.closeDevToolsOnly();
        } else {
            SystemUtils.openUrl(mobUrl);
            log.info("Credentials: {}", selectedVCenter.credentials);
        }
    }
}

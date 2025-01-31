package com.vmware.action.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.vmware.chrome.ChromeDevTools;
import com.vmware.chrome.domain.ApiRequest;
import com.vmware.chrome.domain.ApiResponse;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.Sites;

public abstract class BaseSingleVappJsonAction extends BaseSingleVappAction {

    public BaseSingleVappJsonAction(WorkflowConfig config) {
        super(config);
        super.checkVappJson = true;
    }

    protected void validateListSelection(List values, String propertyName, int selection) {
        if (selection < 1 || selection > values.size()) {
            throw new FatalException("{} selection {} is invalid. Should be between 1 and {}",
                    propertyName, String.valueOf(selection), String.valueOf(values.size()));
        }
    }

    protected void selectVcdCell(Sites.Site site, Integer cellIndex) {
        if (cellIndex == null && site.cells.size() == 1) {
            log.info("Using first cell as there is only one cell");
            vappData.setSelectedVcdCell(site.cells.get(0));
        } else if (cellIndex != null) {
            log.info("Using specified cell index of {}", cellIndex);
            validateListSelection(site.cells, "vcd cell index", cellIndex);
            vappData.setSelectedVcdCell(site.cells.get(cellIndex - 1));
        } else if (vappData.getSelectedVcdCell() != null) {
            log.info("Using already selected cell {}", vappData.getSelectedVcdCell().name);
        } else {
            List<String> cellChoices = site.cells.stream().map(cell -> cell.name).collect(Collectors.toList());
            int index = InputUtils.readSelection(cellChoices, "Select Cell");
            vappData.setSelectedVcdCell(site.cells.get(index));
        }

        Sites.DeployedVM selectedVcdCell = vappData.getSelectedVcdCell();
        String hostName = selectedVcdCell.deployment != null && selectedVcdCell.deployment.ovfProperties != null
                ? selectedVcdCell.deployment.ovfProperties.hostname : selectedVcdCell.endPointURI;
        replacementVariables.addVariable(ReplacementVariables.VariableName.VCD_CELL_NAME, selectedVcdCell.name);
        replacementVariables.addVariable(ReplacementVariables.VariableName.VCD_CELL_HOST_NAME, hostName);
    }

    protected Sites.DeployedVM selectDeployedVm(List<Sites.DeployedVM> deployedVMs, String vmDescription) {
        if (deployedVMs.size() == 1) {
            log.info("Using first {} {} as there is only one {}", vmDescription, deployedVMs.get(0).name, vmDescription);
            return deployedVMs.get(0);
        } else {
            List<InputListSelection> values = deployedVMs.stream().map(vm -> ((InputListSelection) vm)).collect(Collectors.toList());
            int selection = InputUtils.readSelection(values, "Select " + vmDescription);
            return deployedVMs.get(selection);
        }
    }

    protected void openUiUrl(Sites.VmInfo vm) {
        if (fileSystemConfig.autoLogin) {
            openAndLogin(vm);
        } else {
            SystemUtils.openUrl(vm.getUiUrl());
            log.info("Credentials: {}", vm.getLoginCredentials());
        }
    }

    private void openAndLogin(Sites.VmInfo vm) {
        log.info("Opening url {} with chrome and auto logging in", vm.getUiUrl());
        ChromeDevTools devTools = ChromeDevTools.devTools(fileSystemConfig.chromePath, false, fileSystemConfig.chromeDebugPort);
        devTools.sendMessage(new ApiRequest("Page.enable"));
        devTools.sendMessage(ApiRequest.navigate(vm.getUiUrl()));
        ThreadUtils.sleep(2, TimeUnit.SECONDS);

        Map<ApiRequest, Predicate<ApiResponse>> urlOrUsernameInputMap = new HashMap<>();
        urlOrUsernameInputMap.put(ApiRequest.evaluate("window.location.href"),
                response -> response.matchesUrl(vm.getLoggedInUrlPattern()));
        urlOrUsernameInputMap.put(ApiRequest.elementById(vm.getUsernameInputId()),
                response -> response.matchesElementId(vm.getUsernameInputId()));

        ApiResponse response = devTools.waitForAnyPredicate(urlOrUsernameInputMap, 0,
                "logged in url " + vm.getLoggedInUrlPattern() + " or username input " + vm.getUsernameInputId());

        if (response.matchesUrl(vm.getLoggedInUrlPattern())) {
            log.info("Already logged in");
            devTools.closeDevToolsOnly();
            return;
        }

        devTools.setValueById(vm.getUsernameInputId(), vm.getLoginCredentials().username);
        devTools.setValueById(vm.getPasswordInputId(), vm.getLoginCredentials().password);
        devTools.clickByLocator(vm.getLoginButtonLocator(), vm.getLoginButtonTestDescription());
        devTools.closeDevToolsOnly();
    }
}

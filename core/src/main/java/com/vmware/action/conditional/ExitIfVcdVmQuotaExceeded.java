package com.vmware.action.conditional;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.QueryResultVMType;
import com.vmware.vcd.domain.QueryResultVMsType;
import com.vmware.vcd.domain.QueryResultVappType;
import com.vmware.vcd.domain.QuotaPool;
import com.vmware.vcd.domain.QuotaPools;
import com.vmware.vcd.domain.UserSession;
import com.vmware.vcd.domain.UserType;

@ActionDescription("Exit if vm quota exceeded in Vcloud Director (includes vm count for testbeds to be deployed) and not using an existing Vapp.")
public class ExitIfVcdVmQuotaExceeded extends BaseVappAction {
    public ExitIfVcdVmQuotaExceeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(jenkinsConfig.useVappJsonParameter, "useVappJsonParameter is set to true");
    }

    @Override
    public void process() {
        int testbedTemplateVmCount = vappData.getTestbedTemplateVmCount();

        UserSession currentSession = serviceLocator.getVcd().getCurrentSession();
        QuotaPools quotaPools = serviceLocator.getVcd().getQuotaPools(currentSession.user.id);
        QuotaPool runningVMQuota = quotaPools.getRunningVmQuotaPool();

        if (runningVMQuota == null) {
            log.info("No running VM quota found, using vcdVmQuota property instead");
        }

        long poweredOnVmCount = runningVMQuota != null ? runningVMQuota.quotaConsumed : countOfAllVapps();
        int vcdVmQuota = runningVMQuota != null ? runningVMQuota.quotaPoolDefinition.quota : vcdConfig.vcdVmQuota;
        long totalVmCount = poweredOnVmCount + testbedTemplateVmCount;

        if (poweredOnVmCount + testbedTemplateVmCount > vcdVmQuota) {
            cancelWithMessage(String.format("Total Vm count %s (powered on count %s, template count %s) exceeds quota of %s",
                    totalVmCount, poweredOnVmCount, testbedTemplateVmCount, vcdVmQuota));
        } else {
            log.info("Total Vm count {} (powered on count {}, template count {}) does not exceed quota of {}",
                    totalVmCount, poweredOnVmCount, testbedTemplateVmCount, vcdVmQuota);
        }
    }

    private long countOfAllVapps() {
        return vappData.getVapps().stream().filter(QueryResultVappType::isOwnedByWorkflowUser).mapToInt(vapp -> {
            if ("POWERED_ON".equalsIgnoreCase(vapp.status)) {
                return vapp.otherAttributes.numberOfVMs;
            } else if ("MIXED".equalsIgnoreCase(vapp.status)) {
                String vappId = vapp.parseIdFromRef();
                QueryResultVMsType vmsForVapp = serviceLocator.getVcd().queryVmsForVapp(vappId);
                return (int) vmsForVapp.record.stream().filter(QueryResultVMType::isPoweredOn).count();
            } else {
                return 0;
            }
        }).count();
    }
}

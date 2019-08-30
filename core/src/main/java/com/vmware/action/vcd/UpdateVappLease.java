package com.vmware.action.vcd;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.LeaseSection;
import com.vmware.vcd.domain.TaskType;

@ActionDescription("Updates a Vapp runtime lease")
public class UpdateVappLease extends BaseSingleVappAction {

    public UpdateVappLease(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        int timeUnitValue = InputUtils.readSelection(Arrays.asList("Days", "Hours"),
                "Select time unit to use for setting runtime lease");
        TimeUnit timeUnitToUse = timeUnitForSelection(timeUnitValue);
        int leaseExtensionValue = InputUtils.readValueUntilValidInt("Enter extension value in " + timeUnitToUse.name().toLowerCase());

        LeaseSection leaseSection = new LeaseSection();
        leaseSection.deploymentLeaseInSeconds = timeUnitToUse.toSeconds(leaseExtensionValue);

        log.info("Deployment lease in hours {}", timeUnitToUse.toHours(leaseExtensionValue));
        long leaseInMilliseconds = TimeUnit.SECONDS.toMillis(leaseSection.deploymentLeaseInSeconds);
        Date updatedUndeployDate = new Date(new Date().getTime() + leaseInMilliseconds);
        log.info("Runtime lease will be updated to {}", updatedUndeployDate);

        String leaseUrl = vappData.getSelectedVapp().href + "/leaseSettingsSection/";
        Vcd vcd = serviceLocator.getVcd();
        TaskType leaseUpdateTask = vcd.updateResource(leaseUrl, leaseSection);
        vcd.waitForTaskToComplete(leaseUpdateTask.href, 30, TimeUnit.SECONDS);
        vappData.getSelectedVapp().otherAttributes.autoUndeployDate = updatedUndeployDate;
    }

    private TimeUnit timeUnitForSelection(int value) {
        switch (value) {
        case 0:
            return TimeUnit.DAYS;
        default:
            return TimeUnit.HOURS;
        }
    }
}

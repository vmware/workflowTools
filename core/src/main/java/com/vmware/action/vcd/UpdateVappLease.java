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
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.TaskType;

@ActionDescription("Updates a Vapp runtime lease")
public class UpdateVappLease extends BaseSingleVappAction {

    public UpdateVappLease(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        TimeUnit timeUnitToUse = determineTimeUnit();
        int leaseValue = determineLeaseValue(timeUnitToUse);

        LeaseSection leaseSection = new LeaseSection();
        leaseSection.deploymentLeaseInSeconds = timeUnitToUse.toSeconds(leaseValue);

        log.debug("Runtime lease value in hours {}", timeUnitToUse.toHours(leaseValue));
        long leaseInMilliseconds = TimeUnit.SECONDS.toMillis(leaseSection.deploymentLeaseInSeconds);
        Date updatedUndeployDate = new Date(new Date().getTime() + leaseInMilliseconds);
        log.info("Runtime lease will be updated to {}", updatedUndeployDate);

        String leaseUrl = vappData.getSelectedVapp().getSelfLink().href + "/leaseSettingsSection/";
        Vcd vcd = serviceLocator.getVcd();
        TaskType leaseUpdateTask = vcd.updateResource(new LinkType(leaseUrl), leaseSection);
        vcd.waitForTaskToComplete(leaseUpdateTask.href, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        vappData.getSelectedVapp().otherAttributes.autoUndeployDate = updatedUndeployDate;
    }

    private int determineLeaseValue(TimeUnit timeUnitToUse) {
        if (vcdConfig.vappLeaseValue > 0) {
            return vcdConfig.vappLeaseValue;
        }
        return InputUtils.readValueUntilValidInt("Enter lease value in " + timeUnitToUse.name().toLowerCase());
    }

    private TimeUnit determineTimeUnit() {
        if (vcdConfig.vappLeaseValue > 0) {
            return TimeUnit.DAYS;
        }
        int timeUnitValue = InputUtils.readSelection(Arrays.asList("Days", "Hours"),
                "Select time unit to use for setting runtime lease");
        return timeUnitForSelection(timeUnitValue);
    }

    private TimeUnit timeUnitForSelection(int value) {
        if (value == 0) {
            return TimeUnit.DAYS;
        } else {
            return TimeUnit.HOURS;
        }
    }
}

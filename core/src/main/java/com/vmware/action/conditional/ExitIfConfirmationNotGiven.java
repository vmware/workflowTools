package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.exception.CancelException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Exit if the user does not confirm, can be used for confirming commit changes.")
public class ExitIfConfirmationNotGiven extends BaseAction {

    public ExitIfConfirmationNotGiven(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("");
        String confirmation = InputUtils.readValue("Confirm (press ENTER to confirm or type NO to cancel)");
        if (confirmation.equalsIgnoreCase("NO")) {
            throw new CancelException(LogLevel.INFO, "confirmation not given");
        }
    }
}

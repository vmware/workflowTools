package com.vmware.config;

import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ActionAfterFailedPatchCheck implements InputListSelection {

    nothing("Nothing"),
    partialWithGit("Partially apply patch using git"),
    usePatchCommand("Use patch command if cleanly applies"),
    partialWithPatch("Partially apply patch using patch command");

    ActionAfterFailedPatchCheck(String description) {
        this.description = description;
    }

    private String description;

    public String getLabel() {
        return this.name() + " (" + this.description + ")";
    }

    public static ActionAfterFailedPatchCheck fromValue(String value) {
        try {
            return ActionAfterFailedPatchCheck.valueOf(value);
        } catch (IllegalArgumentException iae) {
            throw new FatalException(
                    "Invalid value {}. Valid values from actionAfterFailedPatchCheck are nothing, partial or usePatchCommand", value);
        }
    }

    public static ActionAfterFailedPatchCheck askForAction(boolean usePatchCommand) {
        List<InputListSelection> actions = new ArrayList<>(Arrays.asList(ActionAfterFailedPatchCheck.values()));
        if (usePatchCommand) {
            actions.remove(ActionAfterFailedPatchCheck.usePatchCommand);
        } else {
            actions.remove(ActionAfterFailedPatchCheck.partialWithPatch);
        }
        int selection = InputUtils.readSelection(actions, "Action after failed check");
        return (ActionAfterFailedPatchCheck) actions.get(selection);
    }
}

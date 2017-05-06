package com.vmware.config;

import com.vmware.util.exception.InvalidDataException;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ActionAfterFailedPatchCheck implements InputListSelection {

    nothing("Nothing"),
    partial("Partially apply patch"),
    usePatchCommand("Use patch command");

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
            throw new InvalidDataException(
                    "Invalid value {}. Valid values from actionAfterFailedPatchCheck are nothing, partial or usePatchCommand", value);
        }
    }

    public static ActionAfterFailedPatchCheck askForAction(boolean usePatchCommand) {
        List<InputListSelection> actions = new ArrayList<>();
        actions.addAll(Arrays.asList(ActionAfterFailedPatchCheck.values()));
        if (usePatchCommand) {
            actions.remove(ActionAfterFailedPatchCheck.usePatchCommand);
        }
        int selection = InputUtils.readSelection(actions, "Action after failed check");
        return (ActionAfterFailedPatchCheck) actions.get(selection);
    }
}

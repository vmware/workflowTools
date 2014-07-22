package com.vmware.trello.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.utils.InputListSelection;

public class Board implements InputListSelection {
    public String name;

    @Expose(serialize = false)
    public String id;
    public String idOrganization;
    @Expose(serialize = false)
    public boolean closed;
    @Expose(serialize = false)
    public String url;

    @Expose(deserialize = false)
    @SerializedName("prefs_permissionLevel")
    public String permissionLevel = "private";

    private Board() {}

    public Board(String name) {
        this.name = name;
    }

    @Override
    public String getLabel() {
        return name;
    }
}

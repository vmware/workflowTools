package com.vmware.trello.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.utils.InputListSelection;
import com.vmware.utils.StringUtils;

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

    public void readValues(Board input) {
        if (input == null) {
            return;
        }
        this.name = input.name;
        this.id = input.id;
        this.idOrganization = input.idOrganization;
        this.closed = input.closed;
        this.url = input.url;
        this.permissionLevel = input.permissionLevel;
    }

    public boolean hasId() {
        return StringUtils.isBlank(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof String) {
            String boardName = (String) o;
            return name != null && name.equals(boardName);
        }

        if (o == null || getClass() != o.getClass()) return false;

        Board board = (Board) o;

        if (closed != board.closed) return false;
        if (id != null ? !id.equals(board.id) : board.id != null) return false;
        if (idOrganization != null ? !idOrganization.equals(board.idOrganization) : board.idOrganization != null)
            return false;
        if (name != null ? !name.equals(board.name) : board.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (idOrganization != null ? idOrganization.hashCode() : 0);
        result = 31 * result + (closed ? 1 : 0);
        return result;
    }
}

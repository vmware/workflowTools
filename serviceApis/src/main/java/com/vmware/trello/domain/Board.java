package com.vmware.trello.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.StringUtils;

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

    public BoardMember[] memberships;

    private Board() {}

    public Board(String name) {
        this.name = name;
    }

    @Override
    public String getLabel() {
        return name;
    }

    public boolean hasNoId() {
        return StringUtils.isEmpty(id);
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

    public boolean hasOwner(String id) {
        for (BoardMember boardMember : memberships) {
            if ("admin".equals(boardMember.memberType) && boardMember.idMember.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public BoardMember getFirstOwner() {
        for (BoardMember boardMember : memberships) {
            if ("admin".equals(boardMember.memberType)) {
                return boardMember;
            }
        }
        throw new FatalException("No owner found for board " + name);
    }
}

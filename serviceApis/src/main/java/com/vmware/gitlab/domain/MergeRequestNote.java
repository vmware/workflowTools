package com.vmware.gitlab.domain;

import java.util.Date;

public class MergeRequestNote {
    public long id;
    public String body;
    public User author;
    public Type type;
    public boolean resolvable;
    public boolean resolved;
    public Date createdAt;
    public Date updatedAt;

    public String noteableType;

    public Position position;

    public Date createdDate() {
        return createdAt;
    }

    public boolean isOpenDiffNote() {
        return Type.DiffNote == type && resolvable && !resolved;
    }

    public enum Type {
        DiffNote;
    }

    public class Position {
        public String baseSha;

        public String startSha;
        public String headSha;
        public String positionType;
        public String oldPath;
        public String newPath;

        public Integer oldLine;
        public Integer newLine;
        public LineRange lineRange;
    }

    public class LineRange {
        public LineInfo start;

        public LineInfo end;
    }

    public class LineInfo {

        public String lineCode;
        public Integer oldLine;
        public Integer newLine;
    }
}

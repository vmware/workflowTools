package com.vmware.scm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.vmware.scm.FileChangeType.added;
import static com.vmware.scm.FileChangeType.addedAndModified;
import static com.vmware.scm.FileChangeType.renamed;
import static com.vmware.scm.FileChangeType.renamedAndModified;

public class FileChange {

    private final ScmType scmType;
    private FileChangeType changeType;

    private List<String> filesAffected = new ArrayList<>();

    private int fileVersion;

    public FileChange(ScmType scmType) {
        this.scmType = scmType;
    }

    public FileChange(ScmType scmType, FileChangeType changeType, String... filesAffected) {
        this.scmType = scmType;
        this.changeType = changeType;
        this.filesAffected.addAll(Arrays.asList(filesAffected));
    }

    public void setChangeType(FileChangeType changeType) {
        this.changeType = changeType;
    }

    public void addFileAffected(int index, String name) {
        this.filesAffected.add(index, name);
    }

    public void setFileVersion(int fileVersion) {
        this.fileVersion = fileVersion;
    }

    public FileChangeType getChangeType() {
        return changeType;
    }

    public String getFileAffected(int index) {
        return filesAffected.get(index);
    }

    public String getLastFileAffected() {
        return filesAffected.get(filesAffected.size() - 1);
    }

    public void replaceFileAffected(int index, String value) {
        filesAffected.remove(index);
        filesAffected.add(index, value);
    }

    public void removeFileAffected(int index) {
        filesAffected.remove(index);
    }

    public int getFileVersion() {
        return fileVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileChange that = (FileChange) o;
        FileChangeType changeTypeToUseForComparision = that.changeType;
        if (scmType == ScmType.perforce && that.scmType == ScmType.git) {
            if (that.changeType == renamedAndModified) {
                changeTypeToUseForComparision = renamed;
            } else if (that.changeType == addedAndModified) {
                changeTypeToUseForComparision = added;
            }
        } else if (scmType == ScmType.git && that.scmType == ScmType.perforce) {
            if (changeType == renamedAndModified && that.changeType == renamed) {
                changeTypeToUseForComparision = renamedAndModified;
            } else if (changeType == addedAndModified && that.changeType == added) {
                changeTypeToUseForComparision = addedAndModified;
            }
        }
        return changeType == changeTypeToUseForComparision && Objects.deepEquals(filesAffected, that.filesAffected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeType, filesAffected, fileVersion);
    }

    @Override
    public String toString() {
        String changeDescription = String.format(changeType.getDescription(), filesAffected.toArray());
        if (fileVersion > 0) {
            changeDescription += ", at version " + fileVersion;
        }
        return changeDescription;
    }
}

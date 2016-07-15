package com.vmware.scm;

import com.vmware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.vmware.scm.FileChangeType.added;
import static com.vmware.scm.FileChangeType.deleted;
import static com.vmware.scm.FileChangeType.addedAndModified;
import static com.vmware.scm.FileChangeType.renamed;
import static com.vmware.scm.FileChangeType.renamedAndModified;
import static java.lang.String.format;

public class FileChange {

    public static final String NON_EXISTENT_FILE_IN_GIT = "/dev/null";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final ScmType scmType;
    private FileChangeType changeType;

    private String depotFile;
    private String fileType;
    private String fileMode;
    private List<String> filesAffected = new ArrayList<>();

    private int fileVersion;

    public FileChange(ScmType scmType) {
        this.scmType = scmType;
    }

    public FileChange(ScmType scmType, String fileMode, FileChangeType changeType, String... filesAffected) {
        this.scmType = scmType;
        this.fileMode = fileMode;
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

    public String getFirstFileAffected() {
        return filesAffected.get(0);
    }

    public String getLastFileAffected() {
        return filesAffected.get(filesAffected.size() - 1);
    }

    public String getDepotFile() {
        return depotFile;
    }

    public void setDepotFile(String depotFile) {
        this.depotFile = depotFile;
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

    public String getFileMode() {
        return fileMode;
    }

    public void setFileMode(String fileMode) {
        this.fileMode = fileMode;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
        switch (fileType) {
            case "xtext":
                fileMode = "100755";
                break;
            case "text":
            case "ubinary":
                fileMode = "100644";
                break;
            case "symlink":
                fileMode = "120000";
                break;
            default:
                log.warn("Unrecognized file type {}, setting file mode to 100644", fileType);
                fileMode = "100644";
        }
    }

    public void parseValue(String valueName, String value, String clientNameToStrip) {
        switch (valueName) {
            case "depotFile":
                setDepotFile(value);
                break;
            case "clientFile":
                String strippedPath = value.substring(clientNameToStrip.length());
                addFileAffected(0, strippedPath);
                break;
            case "action":
                setChangeType(FileChangeType.changeTypeFromPerforceValue(value));
                break;
            case "movedFile":
                addFileAffected(0, value);
                break;
            case "rev":
                setFileVersion(Integer.parseInt(value));
                break;
            case "type":
                setFileType(value);
                break;
        }
    }

    public String diffGitLine() {
        String aFile = getFirstFileAffected();
        String bFile = getLastFileAffected();
        String header = format("diff --git a/%s b/%s", aFile, bFile);
        switch (changeType) {
            case renamed:
            case renamedAndModified:
                header += format("\nrename from %s\nrename to %s", aFile, bFile);
                break;
            case added:
            case addedAndModified:
                if (StringUtils.isBlank(fileMode)) {
                throw new RuntimeException("Expected to find file mode for new file " + bFile);
            }
                header += "\nnew file mode " + fileMode;
                break;
            case deleted:
                if (StringUtils.isNotBlank(fileMode)) {
                    header += "\ndeleted file mode " + fileMode;
                } else {
                    log.debug("No deleted file mode found for file {}", aFile);
                }
                break;
        }
        return header;
    }

    public String createMinusPlusLines() {
        String aFile = getFirstFileAffected();
        String bFile = getLastFileAffected();
        switch (changeType) {
            case renamed:
            case renamedAndModified:
            case modified:
                return createGitMinusPlusLine(aFile, bFile);
            case added:
            case addedAndModified:
                return createGitMinusPlusLine(NON_EXISTENT_FILE_IN_GIT, bFile);
            case deleted:
                return createGitMinusPlusLine(aFile, NON_EXISTENT_FILE_IN_GIT);
            default:
                throw new RuntimeException("No match for change type " + changeType);
        }
    }

    private String createGitMinusPlusLine(String aFile, String bFile) {
        aFile = NON_EXISTENT_FILE_IN_GIT.equals(aFile) ? aFile : "a/" + aFile;
        bFile = NON_EXISTENT_FILE_IN_GIT.equals(bFile) ? bFile : "b/" + bFile;
        return format("--- %s\n+++ %s", aFile, bFile);
    }

    public boolean isBinaryFileType() {
        return "ubinary".equals(fileType);
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

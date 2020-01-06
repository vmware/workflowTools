package com.vmware.util.scm;

import com.vmware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.vmware.util.scm.FileChangeType.added;
import static com.vmware.util.scm.FileChangeType.copied;
import static com.vmware.util.scm.FileChangeType.addedAndModified;
import static com.vmware.util.scm.FileChangeType.renamed;
import static com.vmware.util.scm.FileChangeType.renamedAndModified;
import static java.lang.String.format;

public class FileChange {

    public static final String NON_EXISTENT_FILE_IN_GIT = "/dev/null";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private String perforceChangelistId;
    private final ScmType scmType;
    private FileChangeType changeType;

    private String depotFile;
    private String fileType;
    private String fileMode;
    private List<String> filesAffected = new ArrayList<>();

    private int fileVersion;
    private boolean unresolved;

    public FileChange(ScmType scmType) {
        this.scmType = scmType;
    }

    public FileChange(ScmType scmType, FileChangeType changeType, String... filesAffected) {
        this(scmType, null, changeType, filesAffected);
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

    public void addFileAffected(String name) {
        this.filesAffected.add(name);
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

    public String getPerforceChangelistId() {
        return perforceChangelistId;
    }

    public void setPerforceChangelistId(String perforceChangelistId) {
        this.perforceChangelistId = perforceChangelistId;
    }

    public void setFileMode(String fileMode) {
        this.fileMode = fileMode;
    }

    public void setUnresolved(boolean unresolved) {
        this.unresolved = unresolved;
    }

    public boolean isUnresolved() {
        return unresolved;
    }

    public boolean matchesNoneOf(FileChangeType... changeTypes) {
        for (FileChangeType changeType : changeTypes) {
            if (this.changeType == changeType) {
                return false;
            }
        }
        return true;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
        switch (fileType) {
            case "xtext":
            case "kxtext":
            case "xbinary":
            case "cxtext":
            case "xltext":
            case "uxbinary":
            case "xtempobj":
            case "xunicode":
            case "xutf16":
            case "text+x":
                fileMode = "100755";
                break;
            case "text":
            case "text+k":
            case "ktext":
            case "binary":
            case "ctext":
            case "ltext":
            case "ubinary":
            case "tempobj":
                fileMode = "100644";
                break;
            case "symlink":
                fileMode = "120000";
                break;
            default:
                log.warn("Unrecognized file type {} for {}, setting file mode to default value 100644", fileType, getLastFileAffected());
                fileMode = "100644";
        }
    }

    public void parseValue(String valueName, String value, String clientDirectoryToStrip) {
        switch (valueName) {
            case "depotFile":
                setDepotFile(value);
                break;
            case "clientFile":
                String strippedPath = value.substring(clientDirectoryToStrip.length() + 1);
                addFileAffected(strippedPath);
                break;
            case "change":
                setPerforceChangelistId(value);
                break;
            case "action":
                setChangeType(FileChangeType.changeTypeFromPerforceValue(value));
                break;
            case "movedFile":
                addFileAffected(0, value);
                break;
            case "haveRev":
                if ("none".equalsIgnoreCase(value)) {
                    setFileVersion(0);
                } else if (StringUtils.isInteger(value)) {
                    setFileVersion(Integer.parseInt(value));
                }
                break;
            case "type":
                setFileType(value);
                break;
            case "unresolved":
                setUnresolved(true);
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
                if (StringUtils.isEmpty(fileMode)) {
                    throw new RuntimeException("Expected to find file mode for new file " + bFile);
                }
                header += "\nnew file mode " + fileMode;
                break;
            case deleted:
                if (StringUtils.isNotEmpty(fileMode)) {
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
            case integrate:
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
        List<String> filesFromChangeToCheck = filesAffected;
        List<String> filesFromOtherChangeToCheck = that.filesAffected;
        if (scmType == ScmType.perforce && that.scmType == ScmType.git) {
            if (that.changeType == renamedAndModified) {
                changeTypeToUseForComparision = renamed;
            } else if (that.changeType == addedAndModified) {
                changeTypeToUseForComparision = added;
            } else if (that.changeType == copied) {
                changeTypeToUseForComparision = added;
                filesFromOtherChangeToCheck = Collections.singletonList(that.getLastFileAffected());
            }
        } else if (scmType == ScmType.git && that.scmType == ScmType.perforce) {
            if (changeType == renamedAndModified && that.changeType == renamed) {
                changeTypeToUseForComparision = renamedAndModified;
            } else if (changeType == addedAndModified && that.changeType == added) {
                changeTypeToUseForComparision = addedAndModified;
            } else if (changeType == copied && that.changeType == added) {
                changeTypeToUseForComparision = copied;
                filesFromChangeToCheck = Collections.singletonList(getLastFileAffected());
            }
        }
        return changeType == changeTypeToUseForComparision && Objects.deepEquals(filesFromChangeToCheck, filesFromOtherChangeToCheck);
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

    public static boolean containsChangesOfType(List<FileChange> changes, FileChangeType... changeTypes) {
        for (FileChange change : changes) {
            for (FileChangeType changeType : changeTypes) {
                if (change.getChangeType() == changeType) {
                    return true;
                }
            }
        }
        return false;
    }

}

package com.vmware.util.scm.diff;

import java.util.List;

import com.vmware.util.scm.FileChange;

public interface DiffConverter {

    String convert(String diffData);

    byte[] convertAsBytes(String diffData);

    List<FileChange> getFileChanges();
}

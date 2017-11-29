package com.vmware.util.scm.diff;

import com.vmware.util.scm.FileChange;

import java.util.List;

public interface DiffConverter {

    String convert(String diffData);

    byte[] convertAsBytes(String diffData);

    List<FileChange> getFileChanges();
}

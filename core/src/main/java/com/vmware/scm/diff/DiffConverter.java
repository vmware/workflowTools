package com.vmware.scm.diff;

import com.vmware.scm.FileChange;

import java.util.List;

public interface DiffConverter {

    String convert(String diffData);

    byte[] convertAsBytes(String diffData);

    List<FileChange> getFileChanges();

}

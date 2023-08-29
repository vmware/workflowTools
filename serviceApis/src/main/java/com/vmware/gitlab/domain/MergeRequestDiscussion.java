package com.vmware.gitlab.domain;

public class MergeRequestDiscussion {
    public String id;
    public MergeRequestNote[] notes;

    public boolean isOpenDiffDiscussion() {
        return notes != null && notes.length > 0 && notes[0].isOpenDiffNote();
    }
}

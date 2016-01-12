package com.vmware.reviewboard.domain;

import com.vmware.util.input.InputListSelection;

import java.util.Date;

public class ReviewRequestDiff extends BaseEntity implements InputListSelection {

    public int id;

    public String name;

    public int revision;

    public Date timestamp;

    @Override
    public String getLabel() {
        return name;
    }

    public Link getFilesLink() {
        return getLink("files");
    }
}

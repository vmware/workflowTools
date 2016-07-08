package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.util.input.InputListSelection;

import java.util.Date;
import java.util.Map;

public class ReviewRequestDiff extends BaseEntity implements InputListSelection {

    public int id;

    public String name;

    public int revision;

    public Date timestamp;

    @SerializedName("extra_data")
    public Map<String, String> extraData;

    @Override
    public String getLabel() {
        return name;
    }

    public Link getFilesLink() {
        return getLink("files");
    }
}

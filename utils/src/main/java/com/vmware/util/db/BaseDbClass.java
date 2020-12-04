package com.vmware.util.db;

import com.google.gson.annotations.Expose;

public class BaseDbClass {
    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public Long id;
}

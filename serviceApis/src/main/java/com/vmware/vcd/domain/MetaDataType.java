package com.vmware.vcd.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.util.StringUtils;

public class MetaDataType extends ResourceType {
    public String key;

    public TypedValue typedValue;

    public String unescapedValue() {
        return StringUtils.unescapeJavaString(typedValue.value);
    }

    private class TypedValue {
        @SerializedName("_type")
        public String type;

        public String value;
    }
}

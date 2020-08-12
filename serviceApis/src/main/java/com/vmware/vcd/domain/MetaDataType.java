package com.vmware.vcd.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.util.StringUtils;

@VcdMediaType("application/vnd.vmware.vcloud.metadata.value")
public class MetaDataType extends ResourceType {
    public String key;

    public TypedValue typedValue;

    public String unescapedValue() {
        return StringUtils.unescapeJavaString(typedValue.value);
    }

    public static class TypedValue {
        @SerializedName("_type")
        public String type;

        public String value;
    }
}

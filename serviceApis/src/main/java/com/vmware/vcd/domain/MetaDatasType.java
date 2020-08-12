package com.vmware.vcd.domain;

import java.util.List;

@VcdMediaType("application/vnd.vmware.vcloud.metadata")
public class MetaDatasType extends ResourceType {
    public List<MetaDataType> metadataEntry;

    public String jsonMetadata() {
        return getMetadata("json");
    }

    public String getMetadata(String name) {
        return metadataEntry.stream().filter(entry -> entry.key.equalsIgnoreCase(name))
                .map(MetaDataType::unescapedValue).findFirst().orElse(null);
    }
}

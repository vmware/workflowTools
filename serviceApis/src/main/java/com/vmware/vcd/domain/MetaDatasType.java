package com.vmware.vcd.domain;

import java.util.List;

@VcdMediaType("application/vnd.vmware.vcloud.metadata")
public class MetaDatasType extends ResourceType {
    public List<MetaDataType> metadataEntry;

    public String jsonMetadata() {
        return metadataEntry.stream().filter(entry -> entry.key.equalsIgnoreCase("json"))
                .map(MetaDataType::unescapedValue).findFirst().orElse(null);
    }
}

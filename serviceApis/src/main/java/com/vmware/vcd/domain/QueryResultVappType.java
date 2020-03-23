package com.vmware.vcd.domain;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputListSelection;

public class QueryResultVappType extends ResourceType implements InputListSelection {
    public String ownerName;

    @Expose(serialize = false, deserialize = false)
    private boolean isOwnedByWorkflowUser;

    @Expose(serialize = false, deserialize = false)
    private boolean jsonFileBased;

    @Expose(serialize = false, deserialize = false)
    private String jsonData;

    @Expose(serialize = false, deserialize = false)
    private List<Sites.Site> vcdSites = new ArrayList<>();

    public String status;

    public OtherAttributes otherAttributes;

    @Expose(serialize = false, deserialize = false)
    public int poweredOnVmCount;

    public QueryResultVappType() {
    }

    public QueryResultVappType(File jsonFile) {
        this(jsonFile.getName(), jsonFile.getPath());
    }

    public QueryResultVappType(String name, String path) {
        this.name = name;
        this.status = "Json File Based";
        this.jsonFileBased = true;
        this.isOwnedByWorkflowUser = false;
        parseJsonFile(path);
    }

    private void parseJsonFile(String path) {
        String jsonData = IOUtils.read(path);
        parseJson(jsonData);
        this.otherAttributes = new OtherAttributes();
        this.otherAttributes.numberOfVMs = MatcherUtils.allMatches(jsonData, "(\"deployment\"\\s*:)").size();
    }

    @Override
    public String getLabel() {
        String label = name + " (" + status + ") VM Count: " + otherAttributes.numberOfVMs;
        if ("MIXED".equalsIgnoreCase(status)) {
            label += " Powered On: " + poweredOnVmCount;
        }
        if (!jsonFileBased && !isOwnedByWorkflowUser) {
            label = "Shared, owner " + ownerName + " - " + label + " Expires: " + otherAttributes.autoUndeployDate;
        } else if (!jsonFileBased) {
            label += " Expires: " + otherAttributes.autoUndeployDate;
        }
        return label;
    }

    public boolean isOwnedByWorkflowUser() {
        return isOwnedByWorkflowUser;
    }

    public void setOwnedByWorkflowUser(boolean ownedByWorkflowUser) {
        isOwnedByWorkflowUser = ownedByWorkflowUser;
    }

    public boolean isJsonFileBased() {
        return jsonFileBased;
    }

    public void parseJson(String jsonData) {
        Gson gson = new ConfiguredGsonBuilder().build();
        Sites sites = gson.fromJson(jsonData, Sites.class);
        if (sites.sites == null || sites.sites.isEmpty()) {
            throw new FatalException("No sites defined for vapp {}", name);
        }
        this.jsonData = jsonData;
        this.vcdSites.clear();
        this.vcdSites.addAll(sites.sites);
    }

    public boolean jsonDataLoaded() {
        return jsonData != null;
    }

    public List<Sites.Site> getVcdSites() {
        return vcdSites;
    }

    public String getJsonData() {
        return jsonData;
    }

    public class OtherAttributes {
        public int numberOfVMs;

        public Date autoUndeployDate;
    }
}

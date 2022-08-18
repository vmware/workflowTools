package com.vmware.action.vcd;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.StringUtils;
import com.vmware.vcd.domain.QueryResultVMType;
import com.vmware.vcd.domain.QueryResultVMsType;
import com.vmware.vcd.domain.QueryResultVappType;
import com.vmware.vcd.domain.QueryResultVappsType;


@ActionDescription(value = "Loads a list of vapps from Vcloud Director owned by the logged in user.",
        configFlagsToAlwaysExcludeFromCompleter = {"--vcd-refresh-token-name", "--disable-vcd-refresh"})
public class LoadVapps extends BaseVappAction {

    private static final Gson gson = new ConfiguredGsonBuilder().build();

    public LoadVapps(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (!vcdConfig.useOwnedVappsOnly) {
            super.skipActionIfTrue(StringUtils.isNotEmpty(vcdConfig.vappJsonFile), "vappJsonFile has been specified");
            super.skipActionIfTrue(jenkinsConfig.hasConfiguredArtifact(),"jenkins job artifact has been specified");
        }
    }

    @Override
    public void process() {
        QueryResultVappsType vappRecords = serviceLocator.getVcd().queryVapps(vcdConfig.queryFilters());
        List<QueryResultVappType> vapps = new ArrayList<>();
        vapps.addAll(parseVappJsonFiles());
        vapps.addAll(vappRecords.record);
        vappData.setVapps(vapps);
    }



    private Collection<? extends QueryResultVappType> parseVappJsonFiles() {
        if (vcdConfig.vappJsonFiles == null) {
            return Collections.emptySet();
        }
        return vcdConfig.vappJsonFiles.stream().map(this::vappForJsonFile).collect(Collectors.toList());
    }

    private QueryResultVappType vappForJsonFile(String filePath) {
        return new QueryResultVappType(new File(filePath));
    }
}

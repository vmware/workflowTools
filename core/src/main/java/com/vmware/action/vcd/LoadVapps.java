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
import com.vmware.vcd.domain.VappType;


@ActionDescription("Loads a list of vapps from Vcloud Director owned by the logged in user.")
public class LoadVapps extends BaseVappAction {

    private static final Gson gson = new ConfiguredGsonBuilder().build();

    public LoadVapps(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isNotEmpty(vcdConfig.vappJsonFile)) {
            return "vappJsonFile has been specified";
        }
        if (jenkinsConfig.hasConfiguredArtifact()) {
            return "jenkins job artifact has been specified";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        QueryResultVappsType vappRecords = serviceLocator.getVcd().getVapps();
        vappRecords.record.forEach(this::populatedPoweredOnVmCount);
        List<QueryResultVappType> vapps = new ArrayList<>();
        vapps.addAll(parseVappJsonFiles());
        vapps.addAll(vappRecords.record);
        vappData.setVapps(vapps);
    }

    private void populatedPoweredOnVmCount(QueryResultVappType queryResultVappType) {
        if ("POWERED_ON".equalsIgnoreCase(queryResultVappType.status)) {
            queryResultVappType.poweredOnVmCount = queryResultVappType.otherAttributes.numberOfVMs;
        } else if ("MIXED".equalsIgnoreCase(queryResultVappType.status) && queryResultVappType.isOwnedByWorkflowUser()) {
            String vappId = queryResultVappType.parseIdFromRef();
            if (vappId.startsWith("vapp-")) {
                vappId = vappId.substring("vapp-".length());
            }
            QueryResultVMsType vmsForVapp = serviceLocator.getVcd().getVmsForVapp(vappId);
            queryResultVappType.poweredOnVmCount = (int) vmsForVapp.record.stream().filter(QueryResultVMType::isPoweredOn).count();
        }
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

package com.vmware.action.batch;

import com.google.gson.Gson;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.HttpResponse;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.CollectionUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.CancelException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.ApiRequest;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@ActionDescription("Executes the source api call the specified number of times")
public class ExecuteVcdApiRequests extends BaseAction {
    public ExecuteVcdApiRequests(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("fileData", "sourceUrl", "vcdApiVersion", "vcdUsername", "vcdUserPassword");
    }

    @Override
    public void process() {
        String vcdOrg = StringUtils.isNotEmpty(vcdConfig.defaultVcdOrg) ? vcdConfig.defaultVcdOrg : "System";
        Vcd vcdClient = new Vcd(fileSystemConfig.sourceUrl, vcdConfig.vcdApiVersion, vcdConfig.vcdUsername, vcdConfig.vcdUserPassword, vcdOrg);

        Gson gson = new ConfiguredGsonBuilder().build();

        IntStream.rangeClosed(1, fileSystemConfig.repeatCount).forEach(index -> {
            log.info("Execution count {} of {}", index, fileSystemConfig.repeatCount);
            if (index > 1 && config.checkPoint) {
                String confirmation = InputUtils.readValueUntilNotBlank("Execute request (Y/N)");
                if (!"Y".equalsIgnoreCase(confirmation)) {
                    throw new CancelException(LogLevel.INFO, "not running rest of api requests");
                }
            }
            String apiRequest = fileSystemConfig.fileData.replace("$counter", String.valueOf(index));
            ApiRequest request = gson.fromJson(new StringReader(apiRequest), ApiRequest.class);
            HttpResponse response = vcdClient.executeRequest(request);
            List<String> locationHeader = response.getHeaders().get("Location");
            if (CollectionUtils.isNotEmpty(locationHeader) && config.waitForBlockingWorkflowAction) {
                vcdClient.waitForTaskToComplete(locationHeader.get(0), 3, TimeUnit.MINUTES);
            }
            if (config.waitTimeForBlockingWorkflowAction > 0) {
                ThreadUtils.sleep(config.waitTimeForBlockingWorkflowAction, TimeUnit.MILLISECONDS);
            }
        });


    }
}

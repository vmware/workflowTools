package com.vmware.jenkins.domain;

import com.vmware.http.request.RequestParam;

public class JobParameters {
    public JobParameter[] parameter;

    public JobParameters(final JobParameter[] parameter) {
        this.parameter = parameter;
    }

    public RequestParam[] toUrlParams() {
        if (parameter == null) {
            return null;
        }
        RequestParam[] urlParams = new RequestParam[parameter.length];
        int counter = 0;
        for (JobParameter jobParam : parameter) {
            urlParams[counter++] = jobParam.toUrlParam();
        }
        return urlParams;
    }
}

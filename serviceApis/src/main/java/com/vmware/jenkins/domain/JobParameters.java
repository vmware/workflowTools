package com.vmware.jenkins.domain;

import com.vmware.http.request.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JobParameters {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    public List<JobParameter> parameter = new ArrayList<>();

    public JobParameters(final List<JobParameter> parameters) {
        this.parameter.addAll(parameters);
    }

    public RequestParam[] toUrlParams() {
        if (parameter == null) {
            return null;
        }
        RequestParam[] urlParams = new RequestParam[parameter.size()];
        int counter = 0;
        for (JobParameter jobParam : parameter) {
            urlParams[counter++] = jobParam.toUrlParam();
        }
        return urlParams;
    }

}

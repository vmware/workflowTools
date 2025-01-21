package com.vmware.github.domain;

public class GraphqlRequest {

    public GraphqlRequest() {
    }

    public GraphqlRequest(String query) {
        this.query = query;
    }

    public String query;
}

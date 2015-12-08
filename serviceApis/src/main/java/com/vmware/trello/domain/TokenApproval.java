package com.vmware.trello.domain;

public class TokenApproval {

    public String approve = "Allow";
    public String requestKey;
    public String signature;

    public TokenApproval(String requestKey, String signature) {
        this.requestKey = requestKey;
        this.signature = signature;
    }
}

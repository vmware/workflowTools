/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.jira.domain;

public class JiraUser {
    public String name;
    public String displayName;
    public boolean active;

    private JiraUser() {}

    public JiraUser(String name) {
        this.name = name;
    }
}

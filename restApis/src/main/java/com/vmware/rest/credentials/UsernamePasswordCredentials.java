/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.rest.credentials;

public class UsernamePasswordCredentials {
    private String userName;
    private String password;

    public UsernamePasswordCredentials(String username, String password) {
        this.userName = username;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return userName + ":" + password;
    }
}

/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.rest.credentials;

import java.util.HashMap;
import java.util.Map;

public class UsernamePasswordCredentials {
    private String username;
    private String password;

    public UsernamePasswordCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, String> toBugzillaLogin() {
        Map<String, String> values = new HashMap<>();
        values.put("login", username);
        values.put("password", password);
        values.put("rememberlogin", "Bugzilla_remember");
        return values;
    }

    @Override
    public String toString() {
        return username + ":" + password;
    }
}

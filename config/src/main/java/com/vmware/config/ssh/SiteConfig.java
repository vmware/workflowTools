package com.vmware.config.ssh;

import com.vmware.util.StringUtils;

import static com.vmware.util.StringUtils.throwFatalExceptionIfBlank;

public class SiteConfig {

    public String host;

    private int portNumber;

    public String username;

    public String password;

    public SiteConfig() {}

    public SiteConfig(String host, int portNumber, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.portNumber = portNumber;
    }

    public int portNumber() {
        return portNumber == 0 ? 22 : portNumber;
    }

    public void validate() {
        throwFatalExceptionIfBlank(host, "host");
        throwFatalExceptionIfBlank(username, "username");
    }

    @Override
    public String toString() {
        return "SiteConfig{" + "host='" + host + '\'' + ", portNumber=" + portNumber + ", username='" + username + '\'' + '}';
    }
}

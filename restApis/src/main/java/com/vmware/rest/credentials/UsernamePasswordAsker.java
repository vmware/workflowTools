package com.vmware.rest.credentials;

import com.vmware.rest.ApiAuthentication;
import com.vmware.utils.InputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class UsernamePasswordAsker {

    private static UsernamePasswordCredentials testCredentials;
    private static Logger log = LoggerFactory.getLogger(UsernamePasswordAsker.class.getName());


    public static UsernamePasswordCredentials askUserForUsernameAndPassword(ApiAuthentication missingCookie) throws IOException {
        if (testCredentials != null) {
            log.info("Using test credentials");
            return testCredentials;
        }

        log.info("Enter username and password to create {}", missingCookie.getDisplayType());
        log.info("Credentials are only used once for retrieving {}", missingCookie.getDisplayType());

        String username = InputUtils.readValue("Username");
        String password = InputUtils.readPassword("Password");
        return new UsernamePasswordCredentials(username, password);
    }

    public static void setTestCredentials(UsernamePasswordCredentials credentials) {
        UsernamePasswordAsker.testCredentials = credentials;
    }
}

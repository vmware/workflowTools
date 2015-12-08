package com.vmware.rest.credentials;

import com.vmware.rest.cookie.ApiAuthentication;
import com.vmware.utils.input.InputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

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

    public static void setTestCredentials() throws IOException {
        String userHome = System.getProperty( "user.home" );
        File credentialsFile = new File(userHome + File.separator + ".credentials.properties");
        if (!credentialsFile.exists()) {
            return;
        }
        Properties credProps = new Properties();
        credProps.load(new FileReader(credentialsFile));
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(credProps.getProperty("username"), credProps.getProperty("password"));
        UsernamePasswordAsker.testCredentials = credentials;
    }
}

package com.vmware.http.credentials;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class UsernamePasswordAsker {

    private static UsernamePasswordCredentials testCredentials;
    private static Logger log = LoggerFactory.getLogger(UsernamePasswordAsker.class.getName());


    public static UsernamePasswordCredentials askUserForUsernameAndPassword(ApiAuthentication missingApiToken) {
        return askUserForUsernameAndPassword(missingApiToken, null);

    }

    public static UsernamePasswordCredentials askUserForUsernameAndPassword(ApiAuthentication missingCookie, String orgName) {
        if (testCredentials != null) {
            log.info("Using test credentials");
            return testCredentials;
        }

        log.info("Credentials are only used once for retrieving {}", missingCookie.getDisplayType());

        String username = InputUtils.readValue("Username");
        String password = InputUtils.readPassword("Password");

        if (StringUtils.isNotEmpty(orgName) && !username.contains("@")) {
            log.info("Appending org name {} to username {}", orgName, username);
            username += "@" + orgName;
        }
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

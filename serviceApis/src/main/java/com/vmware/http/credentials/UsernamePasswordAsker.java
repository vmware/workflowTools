package com.vmware.http.credentials;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;
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

    public static UsernamePasswordCredentials askUserForUsernameAndPassword(ApiAuthentication missingApiToken, String defaultUsername) {
        return askUserForUsernameAndPassword(missingApiToken, defaultUsername, "Password");
    }

    public static UsernamePasswordCredentials askUserForUsernameAndPassword(ApiAuthentication missingApiToken, String defaultUsername, String passwordLabel) {
        if (testCredentials != null) {
            log.info("Using test credentials");
            return testCredentials;
        }

        log.info("Credentials are only used once for retrieving {}", missingApiToken.getDisplayType());

        String username = InputUtils.readValue("Username (defaults to " + defaultUsername + " if blank)");
        if (StringUtils.isEmpty(username)) {
            log.info("Using default username {}", defaultUsername);
            username = defaultUsername;
        }
        String password = InputUtils.readPassword(passwordLabel);

        return new UsernamePasswordCredentials(username, password);
    }

    public static void setTestCredentials() {
        String userHome = System.getProperty( "user.home" );
        File credentialsFile = new File(userHome + File.separator + ".credentials.properties");
        if (!credentialsFile.exists()) {
            return;
        }
        Properties credProps = new Properties();
        try {
            credProps.load(new FileReader(credentialsFile));
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        UsernamePasswordAsker.testCredentials =
                new UsernamePasswordCredentials(credProps.getProperty("username"), credProps.getProperty("password"));
    }
}

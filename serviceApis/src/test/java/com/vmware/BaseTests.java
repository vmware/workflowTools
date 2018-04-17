package com.vmware;

import java.io.IOException;
import java.util.Properties;

import org.junit.BeforeClass;

import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.util.ClasspathResource;

/**
 * Base class for testing rest api.
 * Unit tests assume that you already have a valid cookie / api token stored for the relevant api.
 * Run workflow AuthenticateAllApis to verify
 */
public class BaseTests {

    protected static Properties testProperties;

    @BeforeClass
    public static void initProperties() throws IOException {
        testProperties = new Properties();
        testProperties.load(new ClasspathResource("/test.properties").getReader());
        UsernamePasswordAsker.setTestCredentials();
    }
}

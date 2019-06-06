package com.vmware;

import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.util.ClasspathResource;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.Properties;

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
        testProperties.load(new ClasspathResource("/test.properties", BaseTests.class).getReader());
        UsernamePasswordAsker.setTestCredentials();
    }
}

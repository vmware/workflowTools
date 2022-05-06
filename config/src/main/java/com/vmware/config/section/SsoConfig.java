package com.vmware.config.section;

import java.util.stream.Stream;

import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;

public class SsoConfig {

    @ConfigurableProperty(help = "Path to chrome executable")
    public String chromePath;

    @ConfigurableProperty(commandLine = "--sso-headless", help = "Run chrome in headless mode for SSO. Certificates will not be used.")
    public boolean ssoHeadless;

    @ConfigurableProperty(help = "Javascript to execute to fetch api token")
    public String ssoApiTokenJavaScript;

    @ConfigurableProperty(help = "Id of button / link to click to sign in with SSO")
    public String ssoLoginButtonId;

    @ConfigurableProperty(help = "Id of input field for username when signing in with SSO")
    public String ssoUsernameInputId;

    @ConfigurableProperty(help = "Id of input field for password when signing in with SSO")
    public String ssoPasswordInputId;

    @ConfigurableProperty(help = "Id of input field for sign in button when signing in with SSO")
    public String ssoSignInButtonId;

    public boolean manualLoginConfigPresent() {
        return Stream.of(chromePath, ssoApiTokenJavaScript, ssoLoginButtonId, ssoUsernameInputId, ssoPasswordInputId, ssoSignInButtonId)
                .allMatch(StringUtils::isNotBlank);
    }
}

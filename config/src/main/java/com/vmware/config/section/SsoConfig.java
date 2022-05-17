package com.vmware.config.section;

import java.util.stream.Stream;

import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;

public class SsoConfig {

    @ConfigurableProperty(help = "Path to chrome executable")
    public String chromePath;

    @ConfigurableProperty(help = "Port to use for chrome remote debugging")
    public int chromeDebugPort;

    @ConfigurableProperty(commandLine = "--sso-email", help = "Email address to use for sso, defaults to git user email if not set")
    public String ssoEmail;

    @ConfigurableProperty(commandLine = "--sso-headless", help = "Run chrome in headless mode for SSO. Certificates will not be used.")
    public boolean ssoHeadless;

    @ConfigurableProperty(help = "Id of input field for username when signing in with SSO")
    public String ssoUsernameInputId;

    @ConfigurableProperty(help = "Id of input field for password when signing in with SSO")
    public String ssoPasswordInputId;

    @ConfigurableProperty(help = "Id of input field for sign in button when signing in with SSO")
    public String ssoSignInButtonId;

    @ConfigurableProperty(help = "Id of input field for RSA passcode when signing in with SSO")
    public String ssoPasscodeInputId;

    @ConfigurableProperty(help = "Id of input field for sign in button when signing in with SSO using a RSA passcode")
    public String ssoPasscodeSignInButtonId;

    @ConfigurableProperty(help = "Elements to click to complete signing in with SSO")
    public String[] elementsToClickAfterSignIn;

    public boolean manualLoginConfigPresent() {
        boolean passwordInputConfigured = Stream.of(chromePath, ssoUsernameInputId, ssoPasswordInputId, ssoSignInButtonId)
                .allMatch(StringUtils::isNotBlank);

        boolean passcodeInputConfigured = Stream.of(chromePath, ssoUsernameInputId, ssoPasscodeInputId, ssoPasscodeSignInButtonId)
                .allMatch(StringUtils::isNotBlank);
        return passwordInputConfigured || passcodeInputConfigured;
    }
}

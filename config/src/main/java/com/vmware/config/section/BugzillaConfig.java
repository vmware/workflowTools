package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;

public class BugzillaConfig {

    @ConfigurableProperty(commandLine = "-bugzillaUrl,--bugzilla-url", help = "Url for Bugzilla server")
    public String bugzillaUrl;

    @ConfigurableProperty(commandLine = "-bugzillaTestBug,--bugzilla-test-bug", help = "Bug number to fetch to test user is logged in")
    public int bugzillaTestBug;

    @ConfigurableProperty(commandLine = "-disableBugzilla,--disable-bugzilla", help = "Don't use Bugzilla when checking bug numbers")
    public boolean disableBugzilla;

    @ConfigurableProperty(commandLine = "-bugzillaQuery,--bugzilla-query", help = "Named query in bugzilla to execute for loading assigned bugs")
    public String bugzillaQuery;

    @ConfigurableProperty(commandLine = "-bugzillaPrefix,--bugzilla-prefix", help = "Represents a bug in bugzilla, only the number part will be stored")
    public String bugzillaPrefix;

    public Integer parseBugzillaBugNumber(String bugNumber) {
        if (StringUtils.isInteger(bugNumber)) {
            return Integer.parseInt(bugNumber);
        }

        boolean prefixMatches = StringUtils.isNotEmpty(bugzillaPrefix)
                && bugNumber.toUpperCase().startsWith(bugzillaPrefix.toUpperCase());
        if (!prefixMatches) {
            return null;
        }

        int lengthToStrip = bugzillaPrefix.length();
        if (bugNumber.toUpperCase().startsWith(bugzillaPrefix.toUpperCase() + "-")) {
            lengthToStrip++;
        }

        String numberPart = bugNumber.substring(lengthToStrip);
        if (StringUtils.isInteger(numberPart)) {
            return Integer.parseInt(numberPart);
        } else {
            return null;
        }
    }

    public String bugzillaUrl(int bugNumber) {
        return UrlUtils.addRelativePaths(bugzillaUrl, "show_bug.cgi?id=" + bugNumber);
    }
}

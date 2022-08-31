package com.vmware.vcd.domain;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vmware.util.CollectionUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.input.InputListSelection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sites {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public List<Site> sites;

    public class Site {
        public VcdCell loadBalancer;
        public List<VcdCell> cells;
        public List<VcServer> vcServers;
        public List<NsxTManager> nsxManagers;
        public List<AviController> aviControllers;
        public DatabaseServer databaseServer;

        public List<VmInfo> vms() {
            List<VmInfo> vms = new ArrayList<>();
            if (loadBalancer != null) {
                if (CollectionUtils.isNotEmpty(cells)) {
                    loadBalancer.credentials = cells.get(0).credentials;
                }
                vms.add(loadBalancer);
            }

            Stream.of(cells, vcServers, nsxManagers, aviControllers).filter(Objects::nonNull).flatMap(Collection::stream).forEach(vms::add);
            vms.add(databaseServer);
            vms.removeIf(Objects::isNull);
            return vms;
        }

        public List<DeployedVM> vcVms() {
            return vcServers.stream().map(vc -> ((DeployedVM) vc)).collect(Collectors.toList());
        }

        public List<DeployedVM> nsxTManagerVms() {
            return nsxManagers.stream().map(vc -> ((DeployedVM) vc)).collect(Collectors.toList());
        }

        public List<DeployedVM> aviControllerVms() {
            return aviControllers.stream().map(vc -> ((DeployedVM) vc)).collect(Collectors.toList());
        }
    }

    public class VcdCell extends DeployedVM {

        @Override
        public String getUsernameInputId() {
            return "usernameInput";
        }

        @Override
        public String getPasswordInputId() {
            return "passwordInput";
        }

        @Override
        public String getLoginButtonLocator() {
            return "document.getElementById(\"loginButton\")";
        }

        @Override
        public String getLoginButtonTestDescription() {
            return "#loginButton";
        }

        @Override
        public String getLoggedInUrlPattern() {
            return getUiUrl() + ".+?cloud/organizations.+";
        }

        @Override
        public String getUiUrl() {
            return UrlUtils.addRelativePaths(endPointURI, "provider");
        }
    }

    public class DeployedVM implements VmInfo {
        public String name;

        public String endPointURI;

        public Deployment deployment;

        public Credentials credentials;

        public Credentials osCredentials;

        public Credentials cliCredentials;

        @Override
        public String getLabel() {
            return name + " (" + endPointURI + ")";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getHost() {
            return URI.create(endPointURI).getHost();
        }

        @Override
        public String getUiUrl() {
            return endPointURI;
        }

        @Override
        public Credentials getLoginCredentials() {
            return credentials;
        }

        @Override
        public String getUsernameInputId() {
            return null;
        }

        @Override
        public String getPasswordInputId() {
            return null;
        }

        @Override
        public String getLoginButtonLocator() {
            return null;
        }

        @Override
        public String getLoginButtonTestDescription() {
            return null;
        }

        @Override
        public String getLoggedInUrlPattern() {
            return null;
        }

        @Override
        public Credentials getSshCredentials() {
            return osCredentials;
        }
    }

    public class Deployment {
        public OvfProperties ovfProperties;
        public GuestProperties guestProperties;
    }

    public class Credentials {
        public Credentials() {}

        public Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String username;

        public String password;

        @Override
        public String toString() {
            return username + " / " + password;
        }
    }

    public class OvfProperties {
        public String hostname;
        public String user;
        public String password;
    }

    public class GuestProperties {
        public String adminPassword;
    }

    public class VcServer extends DeployedVM {
        @Override
        public Credentials getSshCredentials() {
            return new Credentials("root", credentials.password);
        }

        @Override
        public String getUsernameInputId() {
            return "username";
        }

        @Override
        public String getPasswordInputId() {
            return "password";
        }

        @Override
        public String getLoginButtonLocator() {
            return "document.getElementById(\"submit\")";
        }

        @Override
        public String getLoginButtonTestDescription() {
            return "#submit";
        }

        @Override
        public String getLoggedInUrlPattern() {
            return getUiUrl() + ".+?app/folder.+";
        }

        @Override
        public String getUiUrl() {
            return UrlUtils.addRelativePaths(endPointURI, "ui");
        }
    }

    public class NsxTManager extends DeployedVM {

        @Override
        public Credentials getSshCredentials() {
            return cliCredentials;
        }
        @Override
        public Credentials getLoginCredentials() {
            return cliCredentials;
        }

        @Override
        public String getUsernameInputId() {
            return "username";
        }

        @Override
        public String getPasswordInputId() {
            return "password";
        }

        @Override
        public String getLoginButtonLocator() {
            return "document.getElementsByTagName(\"button\")[0]";
        }

        @Override
        public String getLoginButtonTestDescription() {
            return "btn-primary";
        }

        @Override
        public String getLoggedInUrlPattern() {
            return getUiUrl() + ".+?app/home/overview";
        }
        
    }

    public class AviController extends DeployedVM {

        @Override
        public String getUsernameInputId() {
            return "clr-form-control-1";
        }

        @Override
        public String getPasswordInputId() {
            return "clr-form-control-2";
        }

        @Override
        public String getLoginButtonLocator() {
            return "document.getElementsByTagName(\"button\")[0]";
        }

        @Override
        public String getLoginButtonTestDescription() {
            return "btn-primary";
        }

        @Override
        public String getLoggedInUrlPattern() {
            return getUiUrl() + ".+?applications/dashboard";
        }
    }

    public class DatabaseServer implements VmInfo {
        public String databaseType;
        public String host;
        public String port;
        public String dbname;
        public Credentials credentials;
        public Deployment deployment;

        public String urlForPattern(String urlPattern) {
            String url = urlPattern.replace("$HOST", host);
            url = url.replace("$PORT", port);
            url = url.replace("$DB_NAME", dbname);
            return url;
        }

        @Override
        public String getName() {
            return dbname;
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public String getUiUrl() {
            return null;
        }

        @Override
        public Credentials getSshCredentials() {
            return new Credentials("root", deployment.guestProperties.adminPassword);
        }

        @Override
        public Credentials getLoginCredentials() {
            return getSshCredentials();
        }

        @Override
        public String getUsernameInputId() {
            return null;
        }

        @Override
        public String getPasswordInputId() {
            return null;
        }

        @Override
        public String getLoginButtonLocator() {
            return null;
        }

        @Override
        public String getLoginButtonTestDescription() {
            return null;
        }

        @Override
        public String getLoggedInUrlPattern() {
            return null;
        }

        @Override
        public String getLabel() {
            return databaseType + " - " + dbname + " (" + host + ")";
        }
    }

    public interface VmInfo extends InputListSelection {
        String getName();
        String getHost();
        String getUiUrl();
        Credentials getSshCredentials();
        Credentials getLoginCredentials();
        String getUsernameInputId();
        String getPasswordInputId();
        String getLoginButtonLocator();
        String getLoginButtonTestDescription();
        String getLoggedInUrlPattern();
    }

}

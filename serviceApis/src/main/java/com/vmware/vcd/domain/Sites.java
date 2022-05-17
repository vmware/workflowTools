package com.vmware.vcd.domain;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        public List<DeployedVM> nsxManagers;
        public List<DeployedVM> aviControllers;
        public DatabaseServer databaseServer;

        public List<VmInfo> vms() {
            List<VmInfo> vms = new ArrayList<>();
            vms.add(loadBalancer);
            Stream.of(cells, vcServers, nsxManagers, aviControllers).filter(Objects::nonNull).flatMap(Collection::stream).forEach(vms::add);
            vms.add(databaseServer);
            vms.removeIf(Objects::isNull);
            return vms;
        }

        public List<DeployedVM> vcVms() {
            return vcServers.stream().map(vc -> ((DeployedVM) vc)).collect(Collectors.toList());
        }
    }

    public class VcdCell extends DeployedVM {
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
        public String getUiUrl() {
            return UrlUtils.addRelativePaths(endPointURI, "ui");
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
        public String getLabel() {
            return databaseType + " - " + dbname + " (" + host + ")";
        }
    }

    public interface VmInfo extends InputListSelection {
        String getName();
        String getHost();
        String getUiUrl();
        Credentials getSshCredentials();
    }

}

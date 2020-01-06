package com.vmware.vcd.domain;

import java.util.List;

import com.vmware.util.input.InputListSelection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sites {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public List<Site> sites;

    public class Site {
        public DeployedVM loadBalancer;
        public List<DeployedVM> cells;
        public List<DeployedVM> vcServers;
        public List<DeployedVM> nsxManagers;
    }

    public class DeployedVM implements InputListSelection {
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
    }

    public class Deployment {
        public OvfProperties ovfProperties;
    }

    public class Credentials {
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

}

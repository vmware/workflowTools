package com.vmware.github.domain;

import com.vmware.AutocompleteUser;

public class User implements AutocompleteUser {
    public String login;
    public String name;
    public String slug;
    public String url;
    public String company;
    public String location;
    public Organization organization;

    public User() {
    }

    public User(String login, String name) {
        this.login = login;
        this.name = name;
    }

    @Override
    public String username() {
        return login;
    }

    @Override
    public String fullName() {
        return name;
    }

    public class Organization {
        public String login;
        public String name;
    }
}

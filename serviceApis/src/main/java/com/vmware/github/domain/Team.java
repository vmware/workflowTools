package com.vmware.github.domain;

import com.vmware.AutocompleteUser;

public class Team implements AutocompleteUser {
    public String name;
    public String slug;
    public String url;

    public Team() {
    }

    public Team(String slug) {
        this.slug = slug;
    }

    @Override
    public String username() {
        return "@" + slug;
    }

    @Override
    public String fullName() {
        return name;
    }
}

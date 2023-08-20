package com.vmware.gitlab.domain;

public class User {
    public long id;
    public String username;
    public String name;

    public User() {
    }

    public User(int id) {
        this.id = id;
    }
}

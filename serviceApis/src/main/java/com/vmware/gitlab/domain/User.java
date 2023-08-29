package com.vmware.gitlab.domain;

import java.util.Objects;

public class User {
    public long id;
    public String username;
    public String name;

    public User() {
    }

    public User(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}

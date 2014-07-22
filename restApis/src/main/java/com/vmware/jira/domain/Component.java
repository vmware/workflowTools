package com.vmware.jira.domain;

public class Component {

    public String id;

    public String name;

    public String description;

    private Component() {
    }

    public Component(String name) {
        this.name = name;
    }
}

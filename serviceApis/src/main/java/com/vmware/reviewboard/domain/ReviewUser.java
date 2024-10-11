package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.AutocompleteUser;

public class ReviewUser implements AutocompleteUser {

    public String username;

    @SerializedName("first_name")
    public String firstName;

    @SerializedName("last_name")
    public String lastName;

    public ReviewUser() {}

    public ReviewUser(String username, String firstName, String lastName) {
        this();
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String fullName() {
        return firstName + " " + lastName;
    }

    public String toString() {
        return username + " (" + fullName() + ")";
    }
}

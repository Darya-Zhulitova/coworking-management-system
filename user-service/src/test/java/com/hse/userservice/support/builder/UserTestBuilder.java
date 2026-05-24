package com.hse.userservice.support.builder;

import com.hse.userservice.feature.user.domain.User;

public class UserTestBuilder {
    private String email = "resident@example.test";
    private String name = "Test Resident";
    private String description = "Test user";
    private String passwordHash = "{noop}password";

    public static UserTestBuilder user() {
        return new UserTestBuilder();
    }

    public UserTestBuilder email(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public UserTestBuilder description(String description) {
        this.description = description;
        return this;
    }

    public UserTestBuilder passwordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public User build() {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setDescription(description);
        user.setPasswordHash(passwordHash);
        return user;
    }
}

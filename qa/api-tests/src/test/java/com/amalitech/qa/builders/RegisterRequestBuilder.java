package com.amalitech.qa.builders;

import com.amalitech.qa.dto.RegisterRequest;
import com.amalitech.qa.testdata.TestDataLoader;

import java.util.UUID;

/**
 * RegisterRequestBuilder creates RegisterRequest objects with defaults loaded from JSON
 * Supports overriding individual fields for negative/edge case tests
 */
public class RegisterRequestBuilder {
    private String name;
    private String email;
    private String password;
    private boolean loadedDefaults = false;

    /**
     * Initialize with defaults from valid-register.json
     */
    private void ensureDefaults() {
        if (!loadedDefaults) {
            RegisterRequest defaults = TestDataLoader.loadTestData("auth/valid-register.json", RegisterRequest.class);
            this.name = defaults.getName();
            this.email = "qa-" + UUID.randomUUID() + "@test.com"; // Make email unique
            this.password = defaults.getPassword();
            this.loadedDefaults = true;
        }
    }

    public RegisterRequestBuilder withName(String name) {
        ensureDefaults();
        this.name = name;
        return this;
    }

    public RegisterRequestBuilder withEmail(String email) {
        ensureDefaults();
        this.email = email;
        return this;
    }

    public RegisterRequestBuilder withPassword(String password) {
        ensureDefaults();
        this.password = password;
        return this;
    }

    public RegisterRequest build() {
        ensureDefaults();
        return new RegisterRequest(name, email, password);
    }
}

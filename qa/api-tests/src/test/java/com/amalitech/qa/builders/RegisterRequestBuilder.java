package com.amalitech.qa.builders;

import com.amalitech.qa.testdata.AuthTestData;
import com.amalitech.qa.tests.auth.RegisterRequest;

public class RegisterRequestBuilder {
    private String name;
    private String email;
    private String password;

    public RegisterRequestBuilder() {
        RegisterRequest defaults = AuthTestData.defaultRegisterRequest();
        this.name = defaults.getName();
        this.email = defaults.getEmail();
        this.password = defaults.getPassword();
    }

    public RegisterRequestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RegisterRequestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public RegisterRequestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public RegisterRequest build() {
        return new RegisterRequest(name, email, password);
    }
}

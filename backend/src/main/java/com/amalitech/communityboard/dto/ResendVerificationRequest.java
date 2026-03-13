package com.amalitech.communityboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;
}

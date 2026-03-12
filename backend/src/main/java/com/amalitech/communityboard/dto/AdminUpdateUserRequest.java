package com.amalitech.communityboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminUpdateUserRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z]{2,19}([ '\\-][A-Za-z]{1,19})*$",
             message = "Name must be a valid full name: letters only, with spaces, hyphens, or apostrophes as separators between parts (each part at most 19 characters)")
    private String name;

    @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
           message = "Email must be a valid email address")
    private String email;


    // Accepted values: "USER" or "ADMIN"
    @Pattern(regexp = "^(USER|ADMIN)$", message = "Role must be USER or ADMIN")
    private String role;
}

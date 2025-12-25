package com.smartvillage.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LoginRequest DTO - Contains only email and password for authentication
 */
@Schema(description = "Login request with email and password")
public class LoginRequest {

    @Schema(
        description = "User email address",
        example = "superadmin@villageorbit.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("email")
    private String email;

    @Schema(
        description = "User password",
        example = "SuperAdmin@123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("password")
    private String password;

    // Constructors
    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

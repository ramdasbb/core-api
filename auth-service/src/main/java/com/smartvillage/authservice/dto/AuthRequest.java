package com.smartvillage.authservice.dto;

public class AuthRequest {
    private String email;
    private String password;
    private String name;

    // when logging in, name may be null. Keep default constructor.

    public AuthRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

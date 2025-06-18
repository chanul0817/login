package com.oauth.login.service;

public interface EmailService {
    void sendVerificationEmail(String to, String verificationUrl);
}

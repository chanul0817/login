package com.oauth.login.service;

import com.oauth.login.domain.user.EmailVerificationToken;
import com.oauth.login.domain.user.User;
import com.oauth.login.domain.user.UserRole;
import com.oauth_login.dto.RegistrationRequest;
import com.oauth.login.exception.UserAlreadyExistsException;
import com.oauth.login.repository.EmailVerificationTokenRepository;
import com.oauth.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    @Transactional
    public void register(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use: " + request.getEmail());
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_USER)
                .isVerified(false)
                .build();
        
        userRepository.save(user);
        
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24)); // 24시간 후 만료
        tokenRepository.save(verificationToken);
        
        // 이메일 전송
        String verificationUrl = "http://localhost:8080/api/auth/verify?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), verificationUrl);
    }
    
    @Transactional
    public boolean verifyEmail(String token) {
        return tokenRepository.findByToken(token)
                .map(verificationToken -> {
                    if (verificationToken.isExpired()) {
                        return false;
                    }
                    
                    User user = verificationToken.getUser();
                    user.verify();
                    userRepository.save(user);
                    tokenRepository.delete(verificationToken);
                    return true;
                })
                .orElse(false);
    }
}

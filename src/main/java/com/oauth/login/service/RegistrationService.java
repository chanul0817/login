package com.oauth.login.service;

import com.oauth.login.domain.user.EmailVerificationToken;
import com.oauth.login.domain.user.User;
import com.oauth.login.domain.user.UserRole;
import com.oauth.login.dto.RegistrationRequest;
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
    
    /**
     * 이메일로 인증번호 전송
     * @param email 대상 이메일 주소
     * @return 생성된 인증 토큰
     */
    @Transactional
    public String sendVerificationEmail(String email) {
        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("이미 가입된 이메일 주소입니다.");
        }
        
        // 기존 토큰이 있으면 삭제
        userRepository.findByEmail(email).ifPresent(user -> {
            tokenRepository.deleteByUser(user);
        });
        
        // 새 토큰 생성 (임시로 6자리 숫자 생성)
        String token = String.format("%06d", (int)(Math.random() * 1000000));
        
        // 토큰 저장 (실제 구현에서는 이메일로 전송)
        User tempUser = User.builder()
                .email(email)
                .password("temp" + System.currentTimeMillis()) // 임시 비밀번호
                .role(UserRole.ROLE_USER)
                .isVerified(false)
                .build();
        
        userRepository.save(tempUser);
        
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(tempUser);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // 10분 후 만료
        tokenRepository.save(verificationToken);
        
        // 이메일 전송 (개발 환경에서는 콘솔에 출력)
        String verificationMessage = String.format(
            "인증번호: %s\n이 인증번호는 10분간 유효합니다.", token);
        emailService.sendVerificationEmail(email, verificationMessage);
        
        return token;
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

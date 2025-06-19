package com.oauth.login.controller;

<<<<<<< HEAD
import com.oauth.login.dto.RegistrationRequest;
import com.oauth.login.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
=======
import com.oauth.login.domain.user.EmailVerificationToken;
import com.oauth.login.domain.user.User;
import com.oauth.login.domain.user.UserRole;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import com.oauth.login.repository.EmailVerificationTokenRepository;
import com.oauth.login.repository.UserRepository;
import com.oauth.login.service.EmailService;
import com.oauth.login.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
>>>>>>> 4256b0b (거의 다했다)

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {
<<<<<<< HEAD
    
    private final RegistrationService registrationService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
        registrationService.register(request);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "회원가입이 완료되었습니다. 이메일을 확인해주세요.");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        boolean isVerified = registrationService.verifyEmail(token);
        
        Map<String, String> response = new HashMap<>();
        if (isVerified) {
            response.put("message", "이메일 인증이 완료되었습니다. 로그인해주세요.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "유효하지 않거나 만료된 토큰입니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }
=======
    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);
    
    private final RegistrationService registrationService;
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String verificationCode) {
        
        if (email == null || email.isEmpty() || 
            password == null || password.isEmpty() || 
            verificationCode == null || verificationCode.isEmpty()) {
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "모든 필수 정보를 입력해주세요.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 이메일 중복 확인
        if (userRepository.existsByEmail(email)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "이미 가입된 이메일 주소입니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 인증번호 확인
        EmailVerificationToken token = tokenRepository.findByToken(verificationCode)
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 인증번호입니다."));
        
        if (token.isExpired()) {
            throw new IllegalStateException("인증번호가 만료되었습니다. 다시 인증해주세요.");
        }
        
        if (!token.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("인증번호가 일치하지 않습니다.");
        }
        
        // 회원가입 처리
        User newUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password)) // 비밀번호 암호화
                .role(UserRole.ROLE_USER)
                .isVerified(true) // 이메일 인증이 완료되었으므로 true로 설정
                .build();
        
        userRepository.save(newUser);
        
        // 임시 사용자 삭제
        userRepository.delete(token.getUser());
        tokenRepository.delete(token);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/send-verification", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> sendVerificationCode(@RequestParam("email") String email) {
        log.info("Received verification request for email: {}", email);
        if (email == null || email.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "이메일을 입력해주세요.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 이메일 전송 로직 (실제 구현은 EmailService에서 처리)
            String token = registrationService.sendVerificationEmail(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "인증번호가 이메일로 전송되었습니다.");
            response.put("token", token); // 개발용으로 토큰 반환 (실제 프로덕션에서는 제거)
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/verify-email")
    @Transactional
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (!tokenOpt.isPresent() || tokenOpt.get().isExpired()) {
            return ResponseEntity.badRequest().body("유효하지 않거나 만료된 인증 링크입니다.");
        }
        
        EmailVerificationToken verificationToken = tokenOpt.get();
        User user = verificationToken.getUser();
        
        // 사용자 이메일 인증 완료 처리
        user.verify();
        userRepository.save(user);
        
        // 사용된 토큰 삭제
        tokenRepository.delete(verificationToken);
        
        return ResponseEntity.ok().body("이메일 인증이 완료되었습니다. 로그인해주세요.");
    }
>>>>>>> 4256b0b (거의 다했다)
}

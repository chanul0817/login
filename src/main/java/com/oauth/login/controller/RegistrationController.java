package com.oauth.login.controller;

import com.oauth.login.domain.user.EmailVerificationToken;
import com.oauth.login.domain.user.User;
import com.oauth.login.domain.user.UserRole;
import com.oauth.login.repository.EmailVerificationTokenRepository;
import com.oauth.login.repository.UserRepository;
import com.oauth.login.service.EmailService;
import com.oauth.login.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {
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
        
        log.info("회원가입 요청 수신 - 이메일: {}", email);
        
        if (email == null || email.isEmpty() || 
            password == null || password.isEmpty() || 
            verificationCode == null || verificationCode.isEmpty()) {
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "모든 필수 정보를 입력해주세요.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 이메일 형식 검증
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "유효하지 않은 이메일 형식입니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 비밀번호 길이 검증 (최소 8자 이상)
        if (password.length() < 8) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "비밀번호는 8자 이상이어야 합니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 인증 토큰 검증
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(verificationCode);
        if (!tokenOpt.isPresent()) {
            log.warn("유효하지 않은 인증번호 시도 - 코드: {}", verificationCode);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "유효하지 않은 인증번호입니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        EmailVerificationToken token = tokenOpt.get();
        log.info("인증 토큰 조회 - ID: {}, 사용자: {}, 만료: {}, 사용됨: {}", 
                token.getId(), token.getUser().getEmail(), token.isExpired(), token.isUsed());
        
        // 토큰 만료 여부 확인
        if (token.isExpired()) {
            log.warn("만료된 토큰 - ID: {}", token.getId());
            tokenRepository.delete(token);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "만료된 인증번호입니다. 다시 인증해주세요.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 이미 사용된 토큰인지 확인
        if (token.isUsed()) {
            log.warn("이미 사용된 토큰 - ID: {}", token.getId());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "이미 사용된 인증번호입니다. 다시 인증해주세요.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 이메일 일치 여부 확인
        if (!token.getUser().getEmail().equals(email)) {
            log.warn("이메일 불일치 - 토큰 이메일: {}, 요청 이메일: {}", token.getUser().getEmail(), email);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "인증받은 이메일과 일치하지 않습니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmailAndIsVerifiedTrue(email)) {
            log.warn("이미 가입된 이메일 - {}", email);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "이미 가입된 이메일 주소입니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            // 사용자 정보 업데이트 및 저장
            User user = token.getUser();
            user.setPassword(passwordEncoder.encode(password));
            user.verify();
            userRepository.save(user);
            log.info("사용자 등록 완료 - ID: {}, 이메일: {}", user.getId(), user.getEmail());
            
            // 토큰을 사용한 것으로 표시 (삭제 대신 업데이트)
            token.setUsed(true);
            tokenRepository.save(token);
            log.info("인증 토큰 사용 처리 - ID: {}", token.getId());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "회원가입이 완료되었습니다. 로그인해주세요.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류 발생 - 이메일: {}", email, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "회원가입 처리 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping(value = "/send-verification", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public ResponseEntity<?> sendVerificationCode(
            @RequestParam("email") String email,
            @RequestHeader(value = "X-Request-Id", required = false) String clientRequestId) {
        
        String requestId = Optional.ofNullable(clientRequestId)
                .orElseGet(() -> UUID.randomUUID().toString().substring(0, 8));
                
        log.info("[{}] 이메일 인증 요청 수신 - 클라이언트 요청 ID: {}, 이메일: {}", 
                requestId, clientRequestId, email);
        
        if (email == null || email.isEmpty()) {
            log.error("[{}] 오류: 이메일이 비어있습니다.", requestId);
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "이메일을 입력해주세요."));
        }
        
        try {
            log.info("[{}] 이메일 중복 확인 중...", requestId);
            // 이미 인증된 이메일인지 확인
            if (userRepository.existsByEmailAndIsVerifiedTrue(email)) {
                log.error("[{}] 오류: 이미 가입된 이메일 주소입니다.", requestId);
                return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "이미 가입된 이메일 주소입니다."));
            }
            
            // 기존 토큰 확인
            Optional<EmailVerificationToken> existingToken = tokenRepository.findByUserEmail(email);
            if (existingToken.isPresent()) {
                EmailVerificationToken token = existingToken.get();
                if (!token.isExpired() && !token.isUsed()) {
                    log.info("[{}] 이미 유효한 인증 토큰이 존재하여 재사용 - 토큰 ID: {}, 만료: {}, 사용됨: {}", 
                            requestId, token.getId(), token.isExpired(), token.isUsed());
                    
                    // 기존 토큰으로 이메일 전송
                    registrationService.sendVerificationEmail(email);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "이미 발송된 인증번호를 재사용합니다.");
                    response.put("token", token.getToken());
                    return ResponseEntity.ok(response);
                } else {
                    log.info("[{}] 기존 토큰이 만료되었거나 사용되었으므로 새로 생성 - 토큰 ID: {}", requestId, token.getId());
                    // 기존 토큰 삭제하고 새로 생성
                    tokenRepository.delete(token);
                }
            }
            
            log.info("[{}] 새 인증 이메일 전송 시작 - 이메일: {}", requestId, email);
            // 새 인증 이메일 전송
            String token = registrationService.sendVerificationEmail(email);
            log.info("[{}] 새 인증 이메일 전송 완료 - 이메일: {}", requestId, email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "인증번호가 이메일로 전송되었습니다.");
            response.put("token", token);
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.error("[" + requestId + "] 오류 발생 (IllegalStateException): " + e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("[" + requestId + "] 예상치 못한 오류 발생: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Collections.singletonMap("error", "인증번호 전송 중 오류가 발생했습니다."));
        } finally {
            log.info("[{}] === /api/auth/send-verification 요청 처리 완료 ===\n", requestId);
        }
    }
    
    @GetMapping("/verify-email")
    @Transactional
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (!tokenOpt.isPresent()) {
            return ResponseEntity.badRequest().body("유효하지 않은 인증 토큰입니다.");
        }
        
        EmailVerificationToken verificationToken = tokenOpt.get();
        
        // 토큰 만료 여부 확인
        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            return ResponseEntity.badRequest().body("만료된 인증 토큰입니다. 다시 인증해주세요.");
        }
        
        // 이미 사용된 토큰인지 확인
        if (verificationToken.isUsed()) {
            return ResponseEntity.badRequest().body("이미 사용된 인증 토큰입니다.");
        }
        
        User user = verificationToken.getUser();
        
        // 사용자 이메일 인증 완료 처리
        user.verify();
        userRepository.save(user);
        
        // 토큰을 사용한 것으로 표시 (삭제 대신 업데이트)
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
        
        return ResponseEntity.ok().body("이메일 인증이 완료되었습니다. 로그인해주세요.");
    }

}

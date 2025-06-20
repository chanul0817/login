package com.oauth.login.service;

import com.oauth.login.domain.user.EmailVerificationToken;
import com.oauth.login.domain.user.User;
import com.oauth.login.domain.user.UserRole;
import com.oauth.login.dto.RegistrationRequest;
import com.oauth.login.exception.UserAlreadyExistsException;
import com.oauth.login.repository.EmailVerificationTokenRepository;
import com.oauth.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Random;

/**
 * Service for handling user registration and email verification
 */
@Service
public class RegistrationService {
    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            EmailVerificationTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }
    
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
        log.info("이메일 전송 시작 - 수신자: {}, URL: {}", user.getEmail(), verificationUrl);
        emailService.sendVerificationEmail(user.getEmail(), verificationUrl);
        log.info("이메일 전송 완료 - 수신자: {}", user.getEmail());
    }
    
    /**
     * 이메일로 인증번호 전송
     * @param email 대상 이메일 주소
     * @return 생성된 인증 토큰
     */
    @Transactional
    public String sendVerificationEmail(String email) {
        log.info("\n=== sendVerificationEmail 호출 ===");
        log.info("요청 이메일: {}", email);
        
        try {
            // 이메일 형식 검증
            if (email == null || !isValidEmail(email)) {
                String errorMsg = "유효하지 않은 이메일 주소입니다: " + email;
                log.error("오류: {}", errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // 인증번호 생성 (6자리 숫자)
            String token = String.format("%06d", new java.util.Random().nextInt(999999));
            log.info("생성된 인증번호: {}, 길이: {}", token, token.length());

            // 사용자 조회 (락을 걸어 동시성 제어)
            User user = userRepository.findByEmailForUpdate(email)
                    .orElseGet(() -> {
                        log.info("새 사용자 생성: {}", email);
                        User newUser = User.builder()
                                .email(email)
                                .password("temp" + System.currentTimeMillis() + UUID.randomUUID())
                                .role(UserRole.ROLE_USER)
                                .isVerified(false)
                                .build();
                        return userRepository.save(newUser);
                    });

            // 여기서는 user가 null일 수 없음 (orElseGet에서 항상 User 객체 반환 보장)
            
            // 이미 인증된 사용자인지 확인 (isVerified는 null이 될 수 없음)
            if (user.getIsVerified()) {
                log.warn("이미 인증된 이메일: {}", email);
                throw new IllegalStateException("이미 인증된 이메일 주소입니다.");
            }

            // 기존 토큰 삭제 (존재하는 경우) 및 결과 로깅
            tokenRepository.deleteByUser(user);
            log.info("기존 토큰 삭제 요청 완료 - 사용자: {}", email);

            // 새 인증 토큰 생성 및 저장
            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(10)) // 10분 후 만료
                    .used(false)
                    .build();

            tokenRepository.save(verificationToken);
            log.info("새 토큰 저장 완료. 토큰 ID: {}, 만료 시간: {}", 
                    verificationToken.getId(), verificationToken.getExpiryDate());

            // 이메일 전송 (트랜잭션 커밋 후에 실행되도록 분리)
            log.info("이메일 전송을 시작하기 전 - 수신자: {}, 토큰: {}, emailService: {}", email, token, emailService);
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("이메일 전송 시작 - 수신자: {}, 토큰: {}", email, token);
                    emailService.sendVerificationEmail(email, token);
                    log.info("이메일 전송 완료 - 수신자: {}", email);
                } catch (Exception e) {
                    log.error("이메일 전송 중 오류 발생 - 수신자: {}", email, e);
                }
            }).exceptionally(ex -> {
                log.error("이메일 전송 비동기 처리 중 예외 발생 - 수신자: {}", email, ex);
                return null;
            });
            log.info("이메일 전송 요청 완료 - 수신자: {}", email);

            return token;

        } catch (Exception e) {
            log.error("이메일 인증 처리 중 오류 발생 - 수신자: {}", email, e);
            throw e;
        }
    }

    /**
     * 이메일 주소의 유효성을 검증합니다.
     * @param email 검증할 이메일 주소
     * @return 유효한 이메일 형식이면 true, 아니면 false
     */
    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    @Transactional
    public boolean verifyEmail(@NotNull String token) {
        log.info("이메일 인증 시도 - 토큰: {}", token);
        
        try {
            // 토큰 조회 (락을 걸어 동시성 제어)
            return tokenRepository.findByToken(token)
                    .map(verificationToken -> {
                        // 토큰에 연결된 사용자 확인
                        User tokenUser = verificationToken.getUser();
                        if (tokenUser == null) {
                            log.error("토큰에 연결된 사용자가 없습니다. 토큰: {}", token);
                            return false;
                        }
                        
                        // 토큰 만료 여부 확인
                        if (verificationToken.isExpired()) {
                            log.warn("만료된 토큰: {}", token);
                            tokenRepository.delete(verificationToken);
                            return false;
                        }
                        
                        // 이미 사용된 토큰인지 확인
                        if (verificationToken.isUsed()) {
                            log.warn("이미 사용된 토큰: {}", token);
                            return false;
                        }
                        
                        // 사용자 조회 (락을 걸어 동시성 제어)
                        String userEmail = tokenUser.getEmail();
                        if (userEmail == null || userEmail.trim().isEmpty()) {
                            log.error("사용자 이메일이 유효하지 않습니다. 토큰: {}", token);
                            return false;
                        }
                        
                        User user = userRepository.findByEmailForUpdate(userEmail)
                                .orElseThrow(() -> {
                                    log.error("사용자를 찾을 수 없습니다. 이메일: {}", userEmail);
                                    return new IllegalStateException("사용자를 찾을 수 없습니다.");
                                });
                        
                        try {
                            // 토큰 사용 처리
                            verificationToken.setUsed(true);
                            tokenRepository.save(verificationToken);
                            log.info("토큰 사용 처리 완료: {}", token);
                            
                            // 사용자 인증 상태 업데이트
                            user.verify();
                            userRepository.save(user);
                            log.info("사용자 인증 완료: {}", userEmail);
                            
                            return true;
                        } catch (Exception e) {
                            log.error("사용자 인증 처리 중 오류 발생 - 사용자: {}", userEmail, e);
                            throw new RuntimeException("사용자 인증 처리 중 오류가 발생했습니다.", e);
                        }
                    })
                    .orElseGet(() -> {
                        log.warn("유효하지 않은 토큰: {}", token);
                        return false;
                    });
                    
        } catch (Exception e) {
            log.error("이메일 인증 처리 중 오류 발생 - 토큰: {}", token, e);
            throw e;
        }
    }
}

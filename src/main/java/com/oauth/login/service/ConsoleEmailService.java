package com.oauth.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("console")  // 'console' 프로필이 활성화된 경우에만 사용
public class ConsoleEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);
    
    /**
     * Sends a verification email to the specified recipient.
     * This method is intended for development environments and outputs the email content to the console.
     *
     * @param to the recipient's email address
     * @param content the verification content (can be a URL or verification code)
     */
    @Override
    public void sendVerificationEmail(String to, String content) {
        log.info("\n=== [ConsoleEmailService] 이메일 전송 시작 ===");
        log.info("수신자 이메일: {}", to);
        log.info("전송 내용: {}", content);
        log.info("내용 타입: {}", (content.startsWith("http") || content.contains("/") ? "URL" : "인증번호"));
        
        if (to == null || to.trim().isEmpty()) {
            log.error("수신자 이메일이 비어있습니다.");
            throw new IllegalArgumentException("수신자 이메일이 비어있습니다.");
        }
        
        if (content == null || content.trim().isEmpty()) {
            log.error("전송 내용이 비어있습니다.");
            throw new IllegalArgumentException("전송 내용이 비어있습니다.");
        }
        
        String emailContent;
        
        try {
            // Check if the content is a URL or a verification code
            if (content.startsWith("http") || content.contains("/")) {
                // It's a URL
                log.info("URL 형식의 인증 이메일 생성");
                emailContent = String.format(
                    "To: %s%n" +
                    "Subject: 이메일 인증%n%n" +
                    "아래 링크를 클릭하여 이메일 인증을 완료해주세요.%n" +
                    "%s%n%n" +
                    "이 링크는 24시간 동안 유효합니다.", to, content);
            } else {
                // It's a verification code
                log.info("인증번호 형식의 이메일 생성");
                emailContent = String.format(
                    "To: %s%n" +
                    "Subject: 이메일 인증번호%n%n" +
                    "인증번호: %s%n%n" +
                    "이 인증번호는 10분간 유효합니다.", to, content);
            }

            log.info("=== 이메일 내용 ===\n{}", emailContent);
            log.info("======================\n");

            // 표준 출력에도 로그 남기기 (로깅 설정에 따라 콘솔에 출력되지 않을 수 있으므로)
            System.out.println("\n=== [ConsoleEmailService] 이메일 전송 완료 ===");
            System.out.println("수신자: " + to);
            System.out.println("인증번호: " + content);
            System.out.println("==============================\n");

        } catch (Exception e) {
            log.error("이메일 전송 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
}

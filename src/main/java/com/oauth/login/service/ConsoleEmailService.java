package com.oauth.login.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Service for sending verification emails to the console (for development purposes)
 */
@Service
@Profile("!prod")
public class ConsoleEmailService implements EmailService {
    
    /**
     * Sends a verification email to the specified recipient.
     * This method is intended for development environments and outputs the email content to the console.
     *
     * @param to the recipient's email address
     * @param content the verification content (can be a URL or verification code)
     */
    @Override
    public void sendVerificationEmail(String to, String content) {
        String emailContent;
        
        // Check if the content is a URL or a verification code
        if (content.startsWith("http") || content.contains("/")) {
            // It's a URL
            emailContent = String.format(
                "To: %s\n" +
                "Subject: 이메일 인증\n\n" +
                "아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n" +
                "%s\n\n" +
                "이 링크는 24시간 동안 유효합니다.", to, content);
        } else {
            // It's a verification code
            emailContent = String.format(
                "To: %s\n" +
                "Subject: 이메일 인증번호\n\n" +
                "인증번호: %s\n\n" +
                "이 인증번호는 10분간 유효합니다.", to, content);
        }
            
        System.out.println("\n=== 이메일 전송 (개발용) ===\n" + emailContent + "\n======================\n");
    }
}

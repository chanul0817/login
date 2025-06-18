package com.oauth.login.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
public class ConsoleEmailService implements EmailService {
    
    @Override
    public void sendVerificationEmail(String to, String verificationUrl) {
        String emailContent = String.format(
            "To: %s\n" +
            "Subject: 이메일 인증\n\n" +
            "아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n" +
            "%s\n\n" +
            "이 링크는 24시간 동안 유효합니다.", to, verificationUrl);
            
        System.out.println("\n=== 이메일 전송 (개발용) ===\n" + emailContent + "\n======================\n");
    }
}

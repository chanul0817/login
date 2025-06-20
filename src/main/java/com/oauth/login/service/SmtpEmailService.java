package com.oauth.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary  // EmailService의 기본 구현체로 설정
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);
    
    private final JavaMailSender mailSender;
    
    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Override
    public void sendVerificationEmail(String to, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            
            if (content.startsWith("http") || content.contains("/")) {
                // URL 형식의 인증 이메일
                message.setSubject("이메일 인증");
                message.setText("아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n" + 
                              content + "\n\n" +
                              "이 링크는 24시간 동안 유효합니다.");
            } else {
                // 인증번호 형식의 이메일
                message.setSubject("이메일 인증번호");
                message.setText("인증번호: " + content + "\n\n" +
                              "이 인증번호는 10분간 유효합니다.");
            }
            
            mailSender.send(message);
            log.info("이메일 전송 완료 - 수신자: {}, 인증번호: {}", to, content);
            
        } catch (Exception e) {
            log.error("이메일 전송 중 오류 발생 - 수신자: {}, 오류: {}", to, e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
}

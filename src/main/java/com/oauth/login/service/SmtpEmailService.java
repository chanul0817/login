package com.oauth.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
        // 요청 컨텍스트에서 Request ID 가져오기 (MDC나 ThreadLocal을 사용할 수 있지만, 여기서는 간단히 생성)
        String requestId = "NO-ID-" + UUID.randomUUID().toString().substring(0, 4);
        
        log.info("[{}] 이메일 전송 요청 수신 - 수신자: {}", requestId, to);
        
        // 스택 트레이스를 로그에 기록하여 호출 경로 확인
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder stackTraceStr = new StringBuilder();
        for (int i = 2; i < Math.min(6, stackTrace.length); i++) { // 상위 몇 개의 스택만 기록
            stackTraceStr.append("\n    at ").append(stackTrace[i]);
        }
        log.info("[{}] 이메일 전송 호출 스택: {}", requestId, stackTraceStr.toString());
        
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
            
            // 실제 이메일 전송
            mailSender.send(message);
            
            log.info("[{}] 이메일 전송 완료 - 수신자: {}, 인증번호: {}", requestId, to, content);
            log.info("[{}] 이메일 제목: {}", requestId, message.getSubject());
            log.info("[{}] 이메일 본문: {}", requestId, message.getText());
            
        } catch (Exception e) {
            log.error("[" + requestId + "] 이메일 전송 중 오류 발생 - 수신자: " + to, e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
}

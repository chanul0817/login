package com.oauth.login.controller;

import com.oauth.login.dto.RegistrationRequest;
import com.oauth.login.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {
    
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
}

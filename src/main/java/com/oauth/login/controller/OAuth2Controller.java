package com.oauth.login.controller;

import com.oauth.login.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OAuth2Controller {

    private final UserService userService;

    /**
     * 메인 페이지 - 로그인 전후 상태에 따라 다른 화면 표시
     */
    @GetMapping("/")
    public String home(Model model, 
                      @RequestParam(required = false) String logout,
                      @RequestParam(required = false) String error) {
        if (logout != null) {
            model.addAttribute("message", "성공적으로 로그아웃되었습니다.");
        }
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "index";
    }
    
    /**
     * 회원가입 페이지
     */
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String verificationCode,
            Model model) {
        
        // In a real application, you would:
        // 1. Verify the verification code
        // 2. Check if email already exists
        // 3. Hash the password
        // 4. Save the user to database
        
        // For now, we'll just do basic validation
        if (email == null || email.isEmpty() || password == null || password.length() < 6) {
            model.addAttribute("error", "이메일과 비밀번호를 정확히 입력해주세요.");
            return "register";
        }
        
        // Check if verification code was provided (in a real app, verify it matches)
        if (verificationCode == null || verificationCode.isEmpty()) {
            model.addAttribute("error", "이메일 인증이 필요합니다.");
            return "register";
        }
        
        // If everything is valid, redirect to loginSuccess
        return "redirect:/loginSuccess";
    }

    /**
     * 로그인 성공 시 호출되는 엔드포인트
     */
    @GetMapping("/loginSuccess")
    public String loginSuccess(
            @AuthenticationPrincipal OAuth2User oauthUser,
            @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
            Model model) {
        
        if (oauthUser == null) {
            log.warn("인증되지 않은 사용자가 로그인 성공 페이지에 접근했습니다.");
            return "redirect:/?error=인증되지 않은 접근입니다.";
        }

        try {
            String registrationId = authorizedClient.getClientRegistration().getRegistrationId();
            log.info("{} 로그인 시도: {}", registrationId, oauthUser.getName());
            
            // 사용자 정보 추출 및 모델에 추가
            model.addAllAttributes(extractUserInfo(oauthUser, registrationId));
            
            // 세션에 사용자 정보 저장 (필요시)
            // session.setAttribute("user", userInfo);
            
            return "loginSuccess";
            
        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생", e);
            return "redirect:/loginFailure?error=로그인 처리 중 오류가 발생했습니다.";
        }
    }

    /**
     * OAuth2 제공자별로 사용자 정보 추출
     */
    private Map<String, Object> extractUserInfo(OAuth2User oauthUser, String registrationId) {
        Map<String, Object> userInfo = new HashMap<>();
        Map<String, Object> attributes = oauthUser.getAttributes();
        
        // 공통 정보
        userInfo.put("provider", registrationId);
        userInfo.put("id", oauthUser.getName());
        
        try {
            switch (registrationId.toLowerCase()) {
                case "naver":
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    if (response != null) {
                        userInfo.put("id", response.get("id"));
                        userInfo.put("name", response.get("name"));
                        userInfo.put("email", response.get("email"));
                        userInfo.put("profileImage", response.get("profile_image"));
                        userInfo.put("nickname", response.get("nickname"));
                    }
                    break;
                    
                case "kakao":
                    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                    Map<String, Object> profile = kakaoAccount != null ? 
                            (Map<String, Object>) kakaoAccount.get("profile") : null;
                    
                    userInfo.put("email", kakaoAccount != null ? kakaoAccount.get("email") : null);
                    userInfo.put("name", profile != null ? profile.get("nickname") : null);
                    userInfo.put("nickname", profile != null ? profile.get("nickname") : null);
                    userInfo.put("profileImage", profile != null ? profile.get("profile_image_url") : null);
                    break;
                    
                case "google":
                default:
                    userInfo.put("id", attributes.get("sub"));
                    userInfo.put("name", attributes.get("name"));
                    userInfo.put("email", attributes.get("email"));
                    userInfo.put("profileImage", attributes.get("picture"));
                    break;
            }
        } catch (Exception e) {
            log.error("사용자 정보 추출 중 오류 발생: {}", e.getMessage(), e);
        }
        
        // null 값 처리 및 기본값 설정
        userInfo.putIfAbsent("name", "사용자");
        userInfo.putIfAbsent("email", "이메일 없음");
        userInfo.putIfAbsent("profileImage", "/images/default-profile.png");
        
        return userInfo;
    }

    /**
     * 로그인 실패 핸들러
     */
    @GetMapping("/loginFailure")
    public String loginFailure(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String message,
            Model model) {
        
        String errorMessage = "로그인 중 오류가 발생했습니다.";
        if (error != null) {
            errorMessage = error;
        } else if (message != null) {
            errorMessage = message;
        }
        
        log.warn("로그인 실패: {}", errorMessage);
        model.addAttribute("error", errorMessage);
        return "loginFailure";
    }

    /**
     * 현재 로그인한 사용자 정보를 JSON으로 반환
     */
    @GetMapping("/api/user")
    @ResponseBody
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Map.of(
                "success", false,
                "message", "인증되지 않은 사용자입니다.",
                "authenticated", false
            );
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("authenticated", true);
        response.put("user", principal.getAttributes());
        
        return response;
    }
    
    /**
     * 인증 실패 시 리다이렉트되는 엔드포인트
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "redirect:/loginFailure?error=접근 권한이 없습니다.";
    }
    
    /**
     * 로그아웃 성공 핸들러
     */
    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "redirect:/?logout=true";
    }
}

package com.oauth.login.service;

import com.oauth.login.domain.user.User;
import com.oauth.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
                
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User is not verified");
        }
        
        return user;
    }

    /**
     * 현재 인증된 사용자의 모든 속성을 가져옵니다.
     * @param authentication 현재 인증 객체
     * @return 사용자 속성 맵
     */
    public Map<String, Object> getAllUserAttributes(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            return oauth2User.getAttributes();
        }
        return Collections.emptyMap();
    }
    
    /**
     * 현재 인증된 사용자의 OAuth2User 객체를 반환합니다.
     * @return OAuth2User 객체 또는 null
     */
    public OAuth2User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            return (OAuth2User) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * 현재 로그인한 사용자의 ID를 반환합니다.
     * @return 사용자 ID 또는 null
     */
    public String getCurrentUserId() {
        OAuth2User user = getCurrentUser();
        if (user != null) {
            // 공급자별로 ID를 가져오는 방법이 다를 수 있음
            String registrationId = user.getAttribute("provider");
            if (registrationId != null) {
                switch (registrationId.toLowerCase()) {
                    case "naver":
                        Map<String, Object> response = user.getAttribute("response");
                        return response != null ? (String) response.get("id") : null;
                    case "kakao":
                        return String.valueOf(user.getAttribute("id"));
                    case "google":
                        return user.getAttribute("sub");
                    default:
                        return user.getName();
                }
            }
        }
        return null;
    }
}

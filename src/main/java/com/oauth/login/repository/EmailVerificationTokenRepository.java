package com.oauth.login.repository;

import com.oauth.login.domain.user.EmailVerificationToken;
import com.oauth.login.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = :user")
    void deleteByUser(@Param("user") User user);
    
    @Query("SELECT t FROM EmailVerificationToken t JOIN t.user u WHERE u.email = :email")
    Optional<EmailVerificationToken> findByUserEmail(@Param("email") String email);
}

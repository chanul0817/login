package com.oauth.login.repository;

import com.oauth.login.domain.user.EmailVerificationToken;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.JpaRepository;
=======
import com.oauth.login.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
>>>>>>> 4256b0b (거의 다했다)

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
<<<<<<< HEAD
=======
    
    @Transactional
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = :user")
    void deleteByUser(@Param("user") User user);
>>>>>>> 4256b0b (거의 다했다)
}

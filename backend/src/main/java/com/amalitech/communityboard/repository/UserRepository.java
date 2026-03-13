package com.amalitech.communityboard.repository;

import com.amalitech.communityboard.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailVerificationToken(String token);
    boolean existsByEmail(String email);
}

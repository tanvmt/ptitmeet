package com.ptithcm.ptitmeet.repositories;

import com.ptithcm.ptitmeet.entity.mysql.User;
import com.ptithcm.ptitmeet.entity.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    Optional<User> findByProviderId(String providerId);
}

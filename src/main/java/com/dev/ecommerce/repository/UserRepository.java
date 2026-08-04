package com.dev.ecommerce.repository;

import com.dev.ecommerce.entity.User;
import com.dev.ecommerce.entity.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Page<User> findByRoles_Name(
            RoleName roleName,
            Pageable pageable
    );

    boolean existsByEmail(String email);
}
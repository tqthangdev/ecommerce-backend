package com.dev.ecommerce.repository;

import com.dev.ecommerce.entity.Role;
import com.dev.ecommerce.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}

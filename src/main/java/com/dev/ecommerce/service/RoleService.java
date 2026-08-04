package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.response.RoleResponse;
import com.dev.ecommerce.entity.enums.RoleName;
import com.dev.ecommerce.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public List<RoleResponse> getAvailableRoles() {
        if (currentUserIsOwner()) {
            return roleRepository.findAll()
                    .stream()
                    .map(RoleResponse::from)
                    .toList();
        }

        return roleRepository.findAll()
                .stream()
                .filter(role -> role.getName() == RoleName.USER)
                .map(RoleResponse::from)
                .toList();
    }

    private boolean currentUserIsOwner() {
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_OWNER")
                );
    }
}
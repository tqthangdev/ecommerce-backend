package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.AdminUserCreateRequest;
import com.dev.ecommerce.dto.request.AdminUserUpdateRequest;
import com.dev.ecommerce.dto.response.AdminUserResponse;
import com.dev.ecommerce.entity.Role;
import com.dev.ecommerce.entity.User;
import com.dev.ecommerce.entity.enums.RoleName;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ErrorMessage;
import com.dev.ecommerce.repository.RoleRepository;
import com.dev.ecommerce.repository.UserRepository;
import com.dev.ecommerce.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;


    @Transactional
    public AdminUserResponse create(AdminUserCreateRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    ErrorMessage.EMAIL_ALREADY_EXISTS
            );
        }


        if (request.getRoles() != null
                && !request.getRoles().isEmpty()) {

            validateRoleAssignment(request.getRoles());
        }


        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build();


        if (request.getRoles() != null
                && !request.getRoles().isEmpty()) {

            user.setRoles(
                    convertRoles(request.getRoles())
            );
        }


        return mapToResponse(
                userRepository.save(user)
        );
    }


    @Transactional
    public AdminUserResponse update(
            Long id,
            AdminUserUpdateRequest request
    ) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorMessage.USER_NOT_FOUND
                        )
                );


        /*
         * OWNER protection
         *
         * ADMIN không được sửa OWNER
         * OWNER chỉ sửa được chính mình
         */
        if (hasOwnerRole(user)
                && !isSameUser(id)) {

            throw new BusinessException(
                    ErrorMessage.OWNER_ACCOUNT_MODIFICATION_DENIED
            );
        }


        /*
         * Không cho disable OWNER
         */
        if (hasOwnerRole(user)
                && Boolean.FALSE.equals(request.getEnabled())) {

            throw new BusinessException(
                    ErrorMessage.OWNER_DISABLE_DENIED
            );
        }


        if (request.getEmail() != null
                && !request.getEmail().equals(user.getEmail())) {


            if (userRepository.existsByEmail(request.getEmail())) {

                throw new BusinessException(
                        ErrorMessage.EMAIL_ALREADY_EXISTS
                );
            }

            user.setEmail(request.getEmail());
        }


        if (request.getFullName() != null) {

            user.setFullName(
                    request.getFullName()
            );
        }


        /*
         * Password bỏ trống => giữ password cũ
         */
        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(
                            request.getPassword()
                    )
            );
        }


        if (request.getEnabled() != null) {

            user.setEnabled(
                    request.getEnabled()
            );
        }


        if (request.getRoles() != null) {


            /*
             * Nếu target là OWNER
             * không được bỏ OWNER role
             */
            if (hasOwnerRole(user)
                    && !request.getRoles().contains("OWNER")) {

                throw new BusinessException(
                        ErrorMessage.OWNER_ROLE_REMOVAL_DENIED
                );
            }


            validateRoleAssignment(
                    request.getRoles()
            );


            user.setRoles(
                    convertRoles(request.getRoles())
            );
        }


        return mapToResponse(
                userRepository.save(user)
        );
    }


    public Page<AdminUserResponse> list(
            Authentication authentication,
            Pageable pageable
    ) {
        User currentUser = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow();

        boolean isOwner = currentUser.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.OWNER);

        if (isOwner) {
            return userRepository.findAll(pageable)
                    .map(this::mapToResponse);
        }

        boolean isAdmin = currentUser.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);

        if (isAdmin) {
            return userRepository.findByRoles_Name(
                    RoleName.USER,
                    pageable
            ).map(this::mapToResponse);
        }

        throw new BusinessException(ErrorMessage.ACCESS_DENIED);
    }


    public AdminUserResponse getById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorMessage.USER_NOT_FOUND
                        )
                );


        return mapToResponse(user);
    }


    private Set<Role> convertRoles(Set<String> roles) {

        return roles.stream()
                .map(RoleName::valueOf)
                .map(roleName ->
                        roleRepository.findByName(roleName)
                                .orElseThrow(() ->
                                        new BusinessException(
                                                ErrorMessage.ROLE_NOT_FOUND
                                        )
                                )
                )
                .collect(Collectors.toSet());
    }


    private void validateRoleAssignment(
            Set<String> roles
    ) {

        /*
         * Chỉ OWNER được cấp OWNER
         */
        if (roles.contains("OWNER")
                && !currentUserIsOwner()) {

            throw new BusinessException(
                    ErrorMessage.OWNER_ROLE_ASSIGNMENT_DENIED
            );
        }


        /*
         * ADMIN không được tự nâng quyền ADMIN
         */
        if (roles.contains("ADMIN")
                && !currentUserIsOwner()) {

            throw new BusinessException(
                    ErrorMessage.ADMIN_ROLE_ASSIGNMENT_DENIED
            );
        }
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


    private boolean isSameUser(Long id) {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();


        if (authentication == null) {
            return false;
        }


        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();


        return principal.getId()
                .equals(id);
    }


    private boolean hasOwnerRole(User user) {

        return user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() == RoleName.OWNER
                );
    }


    private AdminUserResponse mapToResponse(
            User user
    ) {

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .roles(
                        user.getRoles()
                                .stream()
                                .map(role ->
                                        role.getName().name()
                                )
                                .collect(Collectors.toSet())
                )
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
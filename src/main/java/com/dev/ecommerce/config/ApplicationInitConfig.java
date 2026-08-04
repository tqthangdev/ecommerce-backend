package com.dev.ecommerce.config;

import com.dev.ecommerce.entity.Role;
import com.dev.ecommerce.entity.User;
import com.dev.ecommerce.entity.enums.RoleName;
import com.dev.ecommerce.repository.RoleRepository;
import com.dev.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApplicationInitConfig {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.owner.email}")
    private String ownerEmail;

    @Value("${app.owner.password}")
    private String ownerPassword;

    @Value("${app.owner.full-name}")
    private String ownerFullName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.full-name}")
    private String adminFullName;


    @Bean
    CommandLineRunner initUsers() {
        return args -> {
            createOwnerUser();
            createAdminUser();
        };
    }


    @Transactional
    protected void createOwnerUser() {

        if (userRepository.existsByEmail(ownerEmail)) {
            log.info("Owner user already exists ({}), skipping seed.", ownerEmail);
            return;
        }

        Role ownerRole = roleRepository.findByName(RoleName.OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.OWNER)));


        User owner = new User(
                ownerEmail,
                passwordEncoder.encode(ownerPassword),
                ownerFullName
        );

        Set<Role> roles = new HashSet<>();
        roles.add(ownerRole);
        owner.setRoles(roles);

        userRepository.save(owner);

        log.warn(
                "Default OWNER account created: email={}, password={}. " +
                "Please log in and change this password immediately.",
                ownerEmail,
                ownerPassword
        );
    }


    @Transactional
    protected void createAdminUser() {

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists ({}), skipping seed.", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));


        User admin = new User(
                adminEmail,
                passwordEncoder.encode(adminPassword),
                adminFullName
        );

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);

        userRepository.save(admin);

        log.warn(
                "Default ADMIN account created: email={}, password={}. " +
                "Please log in and change this password immediately.",
                adminEmail,
                adminPassword
        );
    }
}
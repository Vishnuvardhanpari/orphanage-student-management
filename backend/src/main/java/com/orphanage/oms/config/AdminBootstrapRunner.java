package com.orphanage.oms.config;

import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.user.repository.RoleRepository;
import com.orphanage.oms.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Ensures default roles exist and seeds the initial administrator when no users exist.
 */
@Component
@Order(1)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private static final UUID ADMIN_ROLE_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID STAFF_ROLE_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            SecurityProperties securityProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureRoles();

        if (userRepository.count() > 0) {
            return;
        }

        SecurityProperties.Bootstrap bootstrap = securityProperties.bootstrap();
        if (!StringUtils.hasText(bootstrap.adminPassword())) {
            log.warn("No users found and OMS_BOOTSTRAP_ADMIN_PASSWORD is empty; skipping admin bootstrap.");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is missing."));

        User admin = User.builder()
                .username(bootstrap.adminUsername())
                .email(bootstrap.adminEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(bootstrap.adminPassword()))
                .authProvider(AuthProvider.LOCAL)
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(admin);
        log.info("Bootstrap administrator '{}' created. Change the password after first login.", admin.getUsername());
    }

    private void ensureRoles() {
        if (roleRepository.findByName(RoleName.ADMIN).isEmpty()) {
            roleRepository.save(Role.builder()
                    .id(ADMIN_ROLE_ID)
                    .name(RoleName.ADMIN)
                    .description("Full system administrator")
                    .createdDate(Instant.now())
                    .build());
        }
        if (roleRepository.findByName(RoleName.STAFF).isEmpty()) {
            roleRepository.save(Role.builder()
                    .id(STAFF_ROLE_ID)
                    .name(RoleName.STAFF)
                    .description("Day-to-day staff operator")
                    .createdDate(Instant.now())
                    .build());
        }
    }
}

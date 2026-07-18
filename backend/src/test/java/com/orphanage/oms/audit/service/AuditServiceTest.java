package com.orphanage.oms.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orphanage.oms.audit.dto.AuditLogResponse;
import com.orphanage.oms.audit.entity.AuditLog;
import com.orphanage.oms.audit.enums.AuditAction;
import com.orphanage.oms.audit.enums.AuditModule;
import com.orphanage.oms.audit.mapper.AuditLogMapper;
import com.orphanage.oms.audit.repository.AuditLogRepository;
import com.orphanage.oms.exception.ApiException;
import com.orphanage.oms.security.UserPrincipal;
import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.AuthProvider;
import com.orphanage.oms.user.enums.RoleName;
import com.orphanage.oms.util.ClientIpResolver;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private ClientIpResolver clientIpResolver;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, auditLogMapper, clientIpResolver);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordUsesCurrentUserAndIp() {
        authenticateAs("admin");
        when(clientIpResolver.resolveCurrent()).thenReturn("127.0.0.1");
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID entityId = UUID.randomUUID();
        auditService.record(AuditModule.STUDENT, AuditAction.CREATED, entityId, "Student created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getModule()).isEqualTo(AuditModule.STUDENT);
        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATED);
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getDescription()).isEqualTo("Student created");
    }

    @Test
    void recordWithExplicitActor() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record(
                AuditModule.AUTH,
                AuditAction.LOGIN,
                UUID.randomUUID(),
                "User logged in via password",
                "staff1",
                "10.0.0.5");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("staff1");
        assertThat(captor.getValue().getIpAddress()).isEqualTo("10.0.0.5");
    }

    @Test
    void listRejectsInvalidDateRange() {
        assertThatThrownBy(() -> auditService.list(
                        null,
                        null,
                        null,
                        null,
                        null,
                        Instant.parse("2026-07-02T00:00:00Z"),
                        Instant.parse("2026-07-01T00:00:00Z"),
                        PageRequest.of(0, 20)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("from must be on or before to");
    }

    @Test
    void listRejectsInvalidSort() {
        assertThatThrownBy(() -> auditService.list(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20, Sort.by("description"))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid sort property");
    }

    @Test
    void getByIdReturnsMappedResponse() {
        UUID id = UUID.randomUUID();
        AuditLog entity = AuditLog.builder()
                .id(id)
                .module(AuditModule.AUTH)
                .action(AuditAction.LOGIN)
                .description("login")
                .username("admin")
                .createdDate(Instant.now())
                .build();
        AuditLogResponse response = new AuditLogResponse(
                id, AuditModule.AUTH, AuditAction.LOGIN, null, "login", "admin", null, entity.getCreatedDate());

        when(auditLogRepository.findById(id)).thenReturn(Optional.of(entity));
        when(auditLogMapper.toResponse(entity)).thenReturn(response);

        assertThat(auditService.getById(id)).isEqualTo(response);
    }

    @Test
    void getByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(auditLogRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getById(id))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Audit log not found");
    }

    @Test
    void listMapsPage() {
        AuditLog entity = AuditLog.builder()
                .id(UUID.randomUUID())
                .module(AuditModule.REPORT)
                .action(AuditAction.GENERATED)
                .description("report")
                .username("admin")
                .createdDate(Instant.now())
                .build();
        AuditLogResponse response = new AuditLogResponse(
                entity.getId(),
                AuditModule.REPORT,
                AuditAction.GENERATED,
                null,
                "report",
                "admin",
                null,
                entity.getCreatedDate());

        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(auditLogMapper.toResponse(entity)).thenReturn(response);

        Page<AuditLogResponse> page = auditService.list(
                null, null, null, null, null, null, null, PageRequest.of(0, 20, Sort.by("createdDate").descending()));

        assertThat(page.getContent()).containsExactly(response);
    }

    private void authenticateAs(String username) {
        Role role = Role.builder().id(UUID.randomUUID()).name(RoleName.ADMIN).build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(username + "@oms.local")
                .passwordHash("hash")
                .authProvider(AuthProvider.LOCAL)
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

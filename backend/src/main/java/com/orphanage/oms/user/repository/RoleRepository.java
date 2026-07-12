package com.orphanage.oms.user.repository;

import com.orphanage.oms.user.entity.Role;
import com.orphanage.oms.user.enums.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence operations for {@link Role}.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}

package com.orphanage.oms.user.repository;

import com.orphanage.oms.user.entity.User;
import com.orphanage.oms.user.enums.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence operations for {@link User}.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE LOWER(u.username) = LOWER(:username)")
    Optional<User> findByUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") UUID id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("""
            SELECT COUNT(u) FROM User u JOIN u.role r
            WHERE r.name = com.orphanage.oms.user.enums.RoleName.ADMIN AND u.enabled = true
            """)
    long countEnabledAdmins();

    @Query(
            value = """
                    SELECT u FROM User u JOIN u.role r
                    WHERE (:search IS NULL OR :search = ''
                        OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:role IS NULL OR r.name = :role)
                      AND (:enabled IS NULL OR u.enabled = :enabled)
                    """,
            countQuery = """
                    SELECT COUNT(u) FROM User u JOIN u.role r
                    WHERE (:search IS NULL OR :search = ''
                        OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:role IS NULL OR r.name = :role)
                      AND (:enabled IS NULL OR u.enabled = :enabled)
                    """)
    Page<User> search(
            @Param("search") String search,
            @Param("role") RoleName role,
            @Param("enabled") Boolean enabled,
            Pageable pageable);
}

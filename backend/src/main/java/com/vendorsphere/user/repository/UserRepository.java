package com.vendorsphere.user.repository;

import com.vendorsphere.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);

    Page<User> findByOrganizationId(UUID organizationId, Pageable pageable);

    /**
     * Identifiers of the active users holding one role within one organization, used for the
     * role-addressed notification fan-out of Requirement 28.2. Deactivated accounts are skipped
     * because they cannot read what they are sent.
     */
    @Query("""
           SELECT u.id FROM User u JOIN u.roles r
           WHERE u.organization.id = :organizationId AND r.name = :roleName AND u.active = true
           """)
    List<UUID> findActiveUserIdsByOrganizationIdAndRoleName(
            @Param("organizationId") UUID organizationId, @Param("roleName") String roleName);
}

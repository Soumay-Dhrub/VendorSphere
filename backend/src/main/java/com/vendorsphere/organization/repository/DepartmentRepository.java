package com.vendorsphere.organization.repository;

import com.vendorsphere.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findByOrganizationIdAndActiveTrue(UUID organizationId);

    List<Department> findByOrganizationId(UUID organizationId);

    Optional<Department> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndName(UUID organizationId, String name);
}

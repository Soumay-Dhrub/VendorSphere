package com.vendorsphere.organization.service;

import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.exception.ResourceNotFoundException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.organization.dto.DepartmentRequest;
import com.vendorsphere.organization.dto.DepartmentResponse;
import com.vendorsphere.organization.dto.OrganizationResponse;
import com.vendorsphere.organization.entity.Department;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.DepartmentRepository;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));
        return OrganizationResponse.from(organization);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments(boolean activeOnly) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        List<Department> departments = activeOnly
                ? departmentRepository.findByOrganizationIdAndActiveTrue(orgId)
                : departmentRepository.findByOrganizationId(orgId);
        return departments.stream().map(DepartmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(UUID id) {
        return DepartmentResponse.from(findDepartmentInOrg(id));
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));

        if (departmentRepository.existsByOrganizationIdAndName(orgId, request.name())) {
            throw new BusinessException("Department name already exists");
        }

        Department department = new Department();
        department.setOrganization(organization);
        department.setName(request.name());
        department.setCode(request.code());
        department.setManager(resolveManager(request.managerId(), orgId));

        departmentRepository.save(department);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentRequest request) {
        Department department = findDepartmentInOrg(id);
        UUID orgId = SecurityUtils.getCurrentOrganizationId();

        if (!department.getName().equals(request.name())
                && departmentRepository.existsByOrganizationIdAndName(orgId, request.name())) {
            throw new BusinessException("Department name already exists");
        }

        department.setName(request.name());
        department.setCode(request.code());
        department.setManager(resolveManager(request.managerId(), orgId));

        departmentRepository.save(department);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public DepartmentResponse disableDepartment(UUID id) {
        Department department = findDepartmentInOrg(id);
        department.setActive(false);
        departmentRepository.save(department);
        return DepartmentResponse.from(department);
    }

    private Department findDepartmentInOrg(UUID id) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        return departmentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private User resolveManager(UUID managerId, UUID orgId) {
        if (managerId == null) {
            return null;
        }
        User manager = userRepository.findByIdWithRoles(managerId)
                .orElseThrow(() -> new BusinessException("Manager not found"));
        if (!manager.getOrganization().getId().equals(orgId)) {
            throw new BusinessException("Manager must belong to the same organization");
        }
        return manager;
    }
}

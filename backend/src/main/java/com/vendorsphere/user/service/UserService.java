package com.vendorsphere.user.service;

import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.exception.ResourceNotFoundException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.organization.entity.Department;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.DepartmentRepository;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.dto.CreateUserRequest;
import com.vendorsphere.user.dto.UpdateUserRequest;
import com.vendorsphere.user.dto.UserResponse;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.RoleRepository;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        Page<User> page = userRepository.findByOrganizationId(orgId, pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        User user = findUserInOrganization(id);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();

        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new BusinessException("Email is already registered");
        }

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException("Organization not found", HttpStatus.NOT_FOUND));

        User user = new User();
        user.setOrganization(organization);
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setRoles(resolveRoles(request.roles()));

        if (request.departmentId() != null) {
            Department department = departmentRepository.findByIdAndOrganizationId(request.departmentId(), orgId)
                    .orElseThrow(() -> new BusinessException("Department not found"));
            user.setDepartment(department);
        }

        userRepository.save(user);
        return UserResponse.from(userRepository.findByIdWithRoles(user.getId()).orElseThrow());
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUserInOrganization(id);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.roles() != null) {
            user.setRoles(resolveRoles(request.roles()));
        }
        if (request.departmentId() != null) {
            UUID orgId = SecurityUtils.getCurrentOrganizationId();
            Department department = departmentRepository.findByIdAndOrganizationId(request.departmentId(), orgId)
                    .orElseThrow(() -> new BusinessException("Department not found"));
            user.setDepartment(department);
        }

        userRepository.save(user);
        return UserResponse.from(userRepository.findByIdWithRoles(user.getId()).orElseThrow());
    }

    private User findUserInOrganization(UUID id) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (!user.getOrganization().getId().equals(orgId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private Set<Role> resolveRoles(java.util.List<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new BusinessException("Invalid role: " + roleName));
            roles.add(role);
        }
        return roles;
    }

    private PageResponse<UserResponse> toPageResponse(Page<User> page) {
        return new PageResponse<>(
                page.getContent().stream().map(UserResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}

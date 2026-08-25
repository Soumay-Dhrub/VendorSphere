package com.vendorsphere.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.vendor.dto.VendorCategoryRequest;
import com.vendorsphere.vendor.dto.VendorCategoryResponse;
import com.vendorsphere.vendor.entity.VendorCategory;
import com.vendorsphere.vendor.repository.VendorCategoryRepository;
import com.vendorsphere.vendor.repository.VendorRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class VendorCategoryServiceTest {

    private final VendorCategoryRepository vendorCategoryRepository =
            mock(VendorCategoryRepository.class);
    private final VendorRepository vendorRepository = mock(VendorRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);

    private final VendorCategoryService service = new VendorCategoryService(
            vendorCategoryRepository, vendorRepository, organizationRepository);

    private final UUID organizationId = UUID.randomUUID();
    private Organization organization;

    @BeforeEach
    void authenticateCaller() {
        organization = new Organization();
        organization.setId(organizationId);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail("officer@demo-corp.com");
        user.setPasswordHash("hash");

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(organizationRepository.getReferenceById(organizationId)).thenReturn(organization);
        when(vendorCategoryRepository.saveAndFlush(any(VendorCategory.class)))
                .thenAnswer(invocation -> {
                    VendorCategory category = invocation.getArgument(0);
                    if (category.getId() == null) {
                        category.setId(UUID.randomUUID());
                    }
                    return category;
                });
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creationFilesTheCategoryUnderTheCallersOrganization() {
        VendorCategoryResponse response =
                service.create(new VendorCategoryRequest("Hardware", "Servers and laptops"));

        assertThat(response.name()).isEqualTo("Hardware");
        assertThat(response.description()).isEqualTo("Servers and laptops");
        assertThat(response.id()).isNotNull();
        verify(organizationRepository).getReferenceById(organizationId);
    }

    @Test
    void creatingACategoryWhoseNameIsAlreadyHeldIsRejectedWithThePinnedMessage() {
        when(vendorCategoryRepository.existsByOrganizationIdAndNameIgnoreCase(
                organizationId, "hardware")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new VendorCategoryRequest("hardware", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor category already exists")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(vendorCategoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void aUniqueConstraintViolationReportsTheSameConflictAsThePreCheck() {
        when(vendorCategoryRepository.saveAndFlush(any(VendorCategory.class)))
                .thenThrow(new DataIntegrityViolationException("uq_vendor_categories"));

        assertThatThrownBy(() -> service.create(new VendorCategoryRequest("Hardware", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor category already exists")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateRejectsAnotherCategorysNameButAcceptsItsOwnInAnyCase() {
        VendorCategory existing = category("Hardware");
        when(vendorCategoryRepository.findByIdAndOrganizationId(existing.getId(), organizationId))
                .thenReturn(Optional.of(existing));
        when(vendorCategoryRepository.existsByOrganizationIdAndNameIgnoreCase(
                organizationId, "Logistics")).thenReturn(true);

        assertThatThrownBy(() ->
                service.update(existing.getId(), new VendorCategoryRequest("Logistics", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor category already exists")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        VendorCategoryResponse response =
                service.update(existing.getId(), new VendorCategoryRequest("HARDWARE", "Renamed"));

        assertThat(response.name()).isEqualTo("HARDWARE");
        assertThat(response.description()).isEqualTo("Renamed");
        verify(vendorCategoryRepository, never())
                .existsByOrganizationIdAndNameIgnoreCase(organizationId, "HARDWARE");
    }

    @Test
    void deletingACategoryReferencedByVendorsIsRejectedNamingTheCount() {
        VendorCategory existing = category("Hardware");
        when(vendorCategoryRepository.findByIdAndOrganizationId(existing.getId(), organizationId))
                .thenReturn(Optional.of(existing));
        when(vendorRepository.countByOrganizationIdAndCategoryId(organizationId, existing.getId()))
                .thenReturn(3L);

        assertThatThrownBy(() -> service.delete(existing.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor category is referenced by 3 vendors")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        when(vendorRepository.countByOrganizationIdAndCategoryId(organizationId, existing.getId()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.delete(existing.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor category is referenced by 1 vendor");

        verify(vendorCategoryRepository, never()).delete(any());
    }

    @Test
    void deletingAnUnreferencedCategoryRemovesIt() {
        VendorCategory existing = category("Hardware");
        when(vendorCategoryRepository.findByIdAndOrganizationId(existing.getId(), organizationId))
                .thenReturn(Optional.of(existing));
        when(vendorRepository.countByOrganizationIdAndCategoryId(organizationId, existing.getId()))
                .thenReturn(0L);

        service.delete(existing.getId());

        verify(vendorCategoryRepository).delete(existing);
    }

    @Test
    void aCategoryOfAnotherOrganizationIsNotFound() {
        UUID foreignCategoryId = UUID.randomUUID();
        when(vendorCategoryRepository.findByIdAndOrganizationId(foreignCategoryId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.update(foreignCategoryId, new VendorCategoryRequest("Hardware", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor category not found")
                .extracting(thrown -> ((BusinessException) thrown).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThatThrownBy(() -> service.delete(foreignCategoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vendor category not found");

        verify(vendorCategoryRepository, never()).delete(any());
        verify(vendorCategoryRepository, never()).saveAndFlush(any());
    }

    private VendorCategory category(String name) {
        VendorCategory category = new VendorCategory();
        category.setId(UUID.randomUUID());
        category.setOrganization(organization);
        category.setName(name);
        return category;
    }
}

package com.vendorsphere.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VendorRequest(
        @NotBlank @Size(max = 255) String companyName,
        @Size(max = 255) String contactPerson,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        String address,
        @Size(max = 100) String taxIdentifier,
        UUID categoryId
) {
}

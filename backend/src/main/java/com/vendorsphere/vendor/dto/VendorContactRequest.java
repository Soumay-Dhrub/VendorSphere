package com.vendorsphere.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorContactRequest(
        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 100) String designation,
        boolean primaryContact
) {
}

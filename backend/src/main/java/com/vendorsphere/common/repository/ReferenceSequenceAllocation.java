package com.vendorsphere.common.repository;

import java.util.UUID;

public interface ReferenceSequenceAllocation {

    int allocateNextValue(UUID organizationId, String prefix, int year);
}

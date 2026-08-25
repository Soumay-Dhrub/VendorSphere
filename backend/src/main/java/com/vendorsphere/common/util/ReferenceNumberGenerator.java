package com.vendorsphere.common.util;

import java.util.UUID;

public interface ReferenceNumberGenerator {

    String allocate(UUID organizationId, ReferencePrefix prefix);
}

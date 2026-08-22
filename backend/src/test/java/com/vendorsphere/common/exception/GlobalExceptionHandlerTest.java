package com.vendorsphere.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.vendorsphere.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsOptimisticLockFailureToConflictWithPinnedMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleOptimisticLock(
                        new ObjectOptimisticLockingFailureException("vendors", "id"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message())
                .isEqualTo("Record was modified by another user, reload and retry");
    }
}

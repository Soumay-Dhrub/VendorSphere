package com.vendorsphere.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuditPayloadSerializerTest {

    private final AuditPayloadSerializer serializer = new AuditPayloadSerializer();

    @Test
    void serializesNullStateAsNullSoACreationRecordsNoPreviousValue() {
        assertThat(serializer.toJson(null)).isNull();
    }

    @Test
    void serializesAPlainStateMapAsAJsonObject() {
        String json = serializer.toJson(Map.of("status", "ACTIVE"));

        assertThat(json).isEqualTo("{\"status\":\"ACTIVE\"}");
    }

    @Test
    void serializesRecordComponentsByName() {
        String json = serializer.toJson(new StatusChange("PROSPECTIVE", "ACTIVE", "Qualified"));

        assertThat(json)
                .contains("\"previousStatus\":\"PROSPECTIVE\"")
                .contains("\"newStatus\":\"ACTIVE\"")
                .contains("\"reason\":\"Qualified\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "passwordHash", "password", "newPassword", "plainPasswd",
            "tokenHash", "refreshToken", "accessToken",
            "secret", "jwtSecret", "clientCredentials",
            "authorization", "apiKey", "privateKey", "passwordSalt", "otpCode"
    })
    void redactsEveryCredentialShapedProperty(String propertyName) {
        String json = serializer.toJson(Map.of(propertyName, "s3cret-value"));

        assertThat(json).doesNotContain("s3cret-value");
        assertThat(json).contains(AuditPayloadSerializer.REDACTED);
    }

    @Test
    void redactsCredentialsNestedInsideObjectsAndArrays() {
        Object state = Map.of(
                "actor", Map.of("email", "admin@example.test", "passwordHash", "$2a$10$hash"),
                "sessions", List.of(
                        Map.of("device", "laptop", "tokenHash", "aaa"),
                        Map.of("device", "phone", "tokenHash", "bbb")));

        String json = serializer.toJson(state);

        assertThat(json)
                .contains("admin@example.test")
                .contains("laptop")
                .contains("phone")
                .doesNotContain("$2a$10$hash", "aaa", "bbb");
    }

    @Test
    void redactsThePasswordHashOfAnEntityLikeStateObject() {
        String json = serializer.toJson(new AccountState("admin@example.test", "$2a$10$hash"));

        assertThat(json).contains("admin@example.test").doesNotContain("$2a$10$hash");
    }

    @Test
    void redactsPropertiesThatMerelyContainASensitiveFragment() {
        String json = serializer.toJson(Map.of("tokenCount", 3, "passwordUpdatedAt", "2026-01-01"));

        // Substring matching is deliberate, so both of these redact: an over-redacted audit row is
        // a lost detail, an under-redacted one is a permanent leak.
        assertThat(json).contains(AuditPayloadSerializer.REDACTED);
    }

    @Test
    void reportsAnUnserializableStateAsAnIllegalStateSoTheBusinessChangeRollsBack() {
        assertThatThrownBy(() -> serializer.toJson(new Unserializable()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not serialize audit state");
    }

    record StatusChange(String previousStatus, String newStatus, String reason) {
    }

    record AccountState(String email, String passwordHash) {
    }

    static final class Unserializable {

        @SuppressWarnings("unused")
        public String getValue() {
            throw new UnsupportedOperationException("no value");
        }
    }
}

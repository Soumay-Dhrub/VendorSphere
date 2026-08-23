package com.vendorsphere.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns a previous or new state object into the JSON text stored in {@code audit_logs}
 * (Requirement 29.1), with a redaction pass that keeps credentials out of the trail.
 *
 * <h2>Why redaction is not left to callers</h2>
 * Audit rows hold serialized entity state and live forever, so a single careless call such as
 * {@code record(..., user, updatedUser)} would copy a bcrypt hash into permanent storage where the
 * append-only guarantee then makes it impossible to remove. Rather than trusting twenty-two call
 * sites, every serialized document is walked here and any property whose name matches
 * {@link #SENSITIVE_NAME_FRAGMENTS} is replaced with {@link #REDACTED}, at any depth and inside
 * arrays. That covers {@code passwordHash} on {@code User}, {@code tokenHash} on
 * {@code RefreshToken}, the JWT {@code secret} of {@code JwtProperties} and anything similar a
 * future entity introduces, whether or not the caller thought about it.
 *
 * <p>Matching is on the property name rather than the declaring type, so it survives DTOs, maps and
 * nested projections. It is intentionally a substring match: {@code password},
 * {@code passwordHash}, {@code newPassword} and {@code plainPassword} all redact.
 *
 * <p>Callers should still pass purpose-built DTOs, records or maps rather than JPA entities: an
 * entity drags lazy associations into the document and bloats the row even when nothing sensitive
 * survives redaction.
 */
@Component
public class AuditPayloadSerializer {

    /** Value substituted for a sensitive property. */
    public static final String REDACTED = "[REDACTED]";

    /**
     * Lower-case fragments that mark a property as a credential. A property name containing any of
     * them is redacted.
     */
    static final Set<String> SENSITIVE_NAME_FRAGMENTS = Set.of(
            "password",
            "passwd",
            "secret",
            "token",
            "credential",
            "authorization",
            "apikey",
            "privatekey",
            "salt",
            "otp");

    private final ObjectMapper objectMapper;

    public AuditPayloadSerializer() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // A state object without properties is worth recording as {} rather than failing a
                // business transaction, so empty beans do not raise.
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Serializes {@code state} to redacted JSON text, or returns {@code null} when there is nothing
     * to record (which is the normal case for the previous value of a creation).
     *
     * @throws IllegalStateException when {@code state} cannot be serialized. The exception
     *         propagates through {@link AuditService#record}, which runs inside the caller's
     *         transaction, so the business change rolls back and the response is 500 rather than a
     *         silently missing trail entry (Requirement 29.10).
     */
    public String toJson(Object state) {
        if (state == null) {
            return null;
        }
        try {
            JsonNode tree = objectMapper.valueToTree(state);
            return objectMapper.writeValueAsString(redact(tree));
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Could not serialize audit state of type " + state.getClass().getName(), ex);
        }
    }

    /** Replaces the value of every sensitive property in the tree, recursing into objects and arrays. */
    private JsonNode redact(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> sensitiveFields = new ArrayList<>();
            Iterator<String> names = objectNode.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (isSensitive(name)) {
                    sensitiveFields.add(name);
                } else {
                    redact(objectNode.get(name));
                }
            }
            sensitiveFields.forEach(name -> objectNode.put(name, REDACTED));
            return objectNode;
        }
        if (node.isArray()) {
            node.forEach(this::redact);
        }
        return node;
    }

    /** Whether a property name marks a credential. */
    static boolean isSensitive(String propertyName) {
        String normalized = propertyName.toLowerCase(Locale.ROOT);
        return SENSITIVE_NAME_FRAGMENTS.stream().anyMatch(normalized::contains);
    }
}

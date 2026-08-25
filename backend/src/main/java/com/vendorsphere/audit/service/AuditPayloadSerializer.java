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

@Component
public class AuditPayloadSerializer {

    public static final String REDACTED = "[REDACTED]";

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

    static boolean isSensitive(String propertyName) {
        String normalized = propertyName.toLowerCase(Locale.ROOT);
        return SENSITIVE_NAME_FRAGMENTS.stream().anyMatch(normalized::contains);
    }
}

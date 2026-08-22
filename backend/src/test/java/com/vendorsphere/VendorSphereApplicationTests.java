package com.vendorsphere;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.vendorsphere.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test proving the full application context starts: real DataSource, Flyway
 * migrations, JPA entity manager and Spring Data repositories, all against a
 * PostgreSQL 16 container.
 *
 * <p>Because {@code spring.jpa.hibernate.ddl-auto} is {@code validate}, a successful
 * start also asserts that the JPA entity mappings agree with the schema produced by
 * the Flyway migrations ({@code V1__init_schema.sql} and
 * {@code V2__procurement_lifecycle.sql}).
 *
 * <p>Task 22.1 will lift the container declaration below into a shared abstract
 * integration-test base class; it is kept local here on purpose.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class VendorSphereApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
    }

    @Test
    @Transactional
    void migratedSchemaIsQueryable() {
        assertThat(userRepository.count()).isZero();
    }
}

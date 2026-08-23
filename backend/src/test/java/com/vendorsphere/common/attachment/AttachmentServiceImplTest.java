package com.vendorsphere.common.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vendorsphere.auth.security.UserPrincipal;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.entity.Role;
import com.vendorsphere.user.entity.User;
import com.vendorsphere.user.repository.UserRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unit tests for the upload gates of {@link AttachmentServiceImpl} (Requirements 33.3, 33.4, 33.5).
 *
 * <p>No Spring context and no filesystem. The byte store is a recording double, which is what lets
 * these tests assert the gates run <em>before</em> any bytes are written: a rejected upload must
 * leave {@link RecordingStorage#stored} empty.
 *
 * <p>The collaborators are plain test doubles rather than Mockito mocks because the JDK in use is
 * newer than the bytecode instrumentation Mockito relies on, and the whole point of these tests is
 * to run without a container.
 */
class AttachmentServiceImplTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private final List<Attachment> saved = new ArrayList<>();
    private RecordingStorage storage;
    private AttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        storage = new RecordingStorage();
        service = new AttachmentServiceImpl(
                attachmentRepository(), organizationRepository(), userRepository(), storage,
                noAccessPolicies());
        authenticate();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ----- Requirement 33.3: content-type allowlist -----

    static Stream<String> acceptedContentTypes() {
        return AttachmentService.ACCEPTED_CONTENT_TYPES.stream();
    }

    @ParameterizedTest
    @MethodSource("acceptedContentTypes")
    void acceptsEveryAllowlistedContentType(String contentType) {
        AttachmentResponse response = service.upload(
                AttachmentOwnerType.INVOICE, OWNER_ID, file("invoice.bin", contentType, 512));

        assertThat(response.contentType()).isEqualTo(contentType);
        assertThat(storage.stored).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/html", "application/x-sh", "application/octet-stream"})
    @NullSource
    void rejectsContentTypeOutsideTheAllowlistBeforeWritingBytes(String contentType) {
        assertThatThrownBy(() -> service.upload(
                        AttachmentOwnerType.INVOICE, OWNER_ID, file("payload", contentType, 512)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        assertThat(storage.stored).isEmpty();
        assertThat(saved).isEmpty();
    }

    @Test
    void rejectionMessageListsEveryAcceptedContentType() {
        assertThatThrownBy(() -> service.upload(
                        AttachmentOwnerType.INVOICE, OWNER_ID, file("x.html", "text/html", 32)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(exception.getMessage())
                        .contains(AttachmentService.ACCEPTED_CONTENT_TYPES));
    }

    @Test
    void normalizesContentTypeParametersBeforeMatchingTheAllowlist() {
        AttachmentResponse response = service.upload(
                AttachmentOwnerType.RFQ,
                OWNER_ID,
                file("spec.pdf", "application/pdf;charset=UTF-8", 128));

        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(onlySaved().getContentType()).isEqualTo("application/pdf");
    }

    // ----- Requirement 33.4: the 10 MB boundary -----

    @Test
    void acceptsAnUploadOfExactlyTenMegabytes() {
        AttachmentResponse response = service.upload(
                AttachmentOwnerType.QUOTATION,
                OWNER_ID,
                file("big.pdf", "application/pdf", (int) AttachmentService.MAX_BYTE_SIZE));

        assertThat(response.byteSize()).isEqualTo(10_485_760L);
        assertThat(storage.stored).hasSize(1);
    }

    @Test
    void rejectsAnUploadOneByteOverTheLimitBeforeWritingBytes() {
        MultipartFile oversized =
                file("big.pdf", "application/pdf", (int) AttachmentService.MAX_BYTE_SIZE + 1);

        assertThatThrownBy(() -> service.upload(AttachmentOwnerType.QUOTATION, OWNER_ID, oversized))
                .isInstanceOf(BusinessException.class)
                .hasMessage("File exceeds the 10 MB limit")
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

        assertThat(storage.stored).isEmpty();
        assertThat(saved).isEmpty();
    }

    // ----- Requirement 33.5: the original file name stays metadata only -----

    @ParameterizedTest
    @ValueSource(strings = {"../../etc/passwd", "..\\..\\windows\\system32\\config"})
    void retainsOnlyTheLeafOfAHostileFileNameAsMetadata(String hostileName) {
        service.upload(
                AttachmentOwnerType.VENDOR_DOCUMENT,
                OWNER_ID,
                file(hostileName, "application/pdf", 64));

        Attachment attachment = onlySaved();
        assertThat(attachment.getOriginalFilename()).doesNotContain("/", "\\", "..");
        assertThat(attachment.getOriginalFilename())
                .isEqualTo(AttachmentFilenames.sanitize(hostileName));
        assertThat(attachment.getStorageReference()).doesNotContain("passwd", "config");
    }

    @Test
    void storesTheReferenceReturnedByTheByteStoreAndNotTheFileName() {
        service.upload(
                AttachmentOwnerType.DELIVERY_PROOF,
                OWNER_ID,
                file("signed-proof.pdf", "application/pdf", 64));

        Attachment attachment = onlySaved();
        assertThat(attachment.getStorageReference()).isEqualTo(storage.stored.getFirst());
        assertThat(attachment.getStorageReference()).doesNotContain("signed-proof");
        assertThat(attachment.getOriginalFilename()).isEqualTo("signed-proof.pdf");
    }

    // ----- test doubles -----

    /** Records the references it handed out so a rejected upload can be shown to write nothing. */
    private static final class RecordingStorage implements AttachmentStorage {

        private final List<String> stored = new ArrayList<>();

        @Override
        public String store(MultipartFile file) {
            String reference = UUID.randomUUID().toString();
            stored.add(reference);
            return reference;
        }

        @Override
        public Resource load(String storageReference) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String storageReference) {
            throw new UnsupportedOperationException();
        }
    }

    private Attachment onlySaved() {
        assertThat(saved).hasSize(1);
        return saved.getFirst();
    }

    private AttachmentRepository attachmentRepository() {
        return stub(AttachmentRepository.class, (proxy, method, args) -> {
            if ("save".equals(method.getName())) {
                Attachment attachment = (Attachment) args[0];
                saved.add(attachment);
                return attachment;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private OrganizationRepository organizationRepository() {
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        return stub(OrganizationRepository.class, (proxy, method, args) -> {
            if ("getReferenceById".equals(method.getName())) {
                assertThat(args[0]).isEqualTo(ORGANIZATION_ID);
                return organization;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private UserRepository userRepository() {
        User actor = actor();
        return stub(UserRepository.class, (proxy, method, args) -> {
            if ("getReferenceById".equals(method.getName())) {
                assertThat(args[0]).isEqualTo(ACTOR_ID);
                return actor;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    /** No module has registered an owner access policy in this unit test. */
    private static ObjectProvider<AttachmentOwnerAccessPolicy> noAccessPolicies() {
        return stub(ObjectProvider.class, (proxy, method, args) -> {
            if ("stream".equals(method.getName())) {
                return Stream.empty();
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<? super T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                AttachmentServiceImplTest.class.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static void authenticate() {
        UserPrincipal principal = new UserPrincipal(actor());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    private static User actor() {
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);

        Role role = new Role();
        role.setName("PROCUREMENT_OFFICER");

        User user = new User();
        user.setId(ACTOR_ID);
        user.setOrganization(organization);
        user.setEmail("officer@example.test");
        user.setPasswordHash("irrelevant");
        user.setRoles(Set.of(role));
        return user;
    }

    private static MultipartFile file(String name, String contentType, int byteSize) {
        return new MockMultipartFile("file", name, contentType, new byte[byteSize]);
    }
}

package com.vendorsphere.common.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vendorsphere.common.config.AttachmentProperties;
import com.vendorsphere.common.exception.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class FilesystemAttachmentStorageTest {

    @TempDir
    Path baseDirectory;

    private FilesystemAttachmentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FilesystemAttachmentStorage(
                new AttachmentProperties(baseDirectory.toString()));
    }

    @Test
    void referenceIsARandomUuidCarryingNoPartOfTheFileName() throws IOException {
        String reference = storage.store(file("quarterly-quote.pdf", "quote bytes"));

        assertThat(UUID.fromString(reference)).hasToString(reference);
        assertThat(reference).doesNotContain("quarterly", "quote", ".pdf");
        assertThat(Files.readString(baseDirectory.resolve(reference))).isEqualTo("quote bytes");
    }

    @Test
    void twoUploadsOfTheSameFileGetDistinctReferences() {
        String first = storage.store(file("quote.pdf", "same bytes"));
        String second = storage.store(file("quote.pdf", "same bytes"));

        assertThat(first).isNotEqualTo(second);
        assertThat(baseDirectory.resolve(first)).isRegularFile();
        assertThat(baseDirectory.resolve(second)).isRegularFile();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../etc/passwd", "..\\..\\windows\\system32\\config"})
    void hostileFileNameNeitherReachesTheReferenceNorEscapesTheBaseDirectory(String hostileName)
            throws IOException {
        String reference = storage.store(file(hostileName, "hostile bytes"));

        assertThat(reference).doesNotContain("..", "/", "\\", "etc", "passwd", "windows", "config");

        Path written = baseDirectory.resolve(reference);
        assertThat(written.normalize().getParent()).isEqualTo(baseDirectory);
        try (Stream<Path> children = Files.list(baseDirectory)) {
            assertThat(children).containsExactly(written);
        }
    }

    @Test
    void tamperedReferenceIsRejectedAsNotFound() {
        assertThatThrownBy(() -> storage.load("../../etc/passwd"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Attachment not found")
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static MockMultipartFile file(String name, String content) {
        return new MockMultipartFile(
                "file", name, "application/pdf", content.getBytes(StandardCharsets.UTF_8));
    }
}

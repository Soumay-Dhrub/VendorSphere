package com.vendorsphere.common.attachment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Serves stored files. Uploads are exposed by the owning module (vendor documents, purchase request
 * attachments, RFQ documents and so on), which knows the state rules that gate them.
 *
 * <p>The response body is the file itself rather than an {@code ApiResponse} envelope; a binary
 * download is the one boundary where wrapping is not applicable. Failures still leave through
 * {@code GlobalExceptionHandler} and carry the usual envelope.
 */
@RestController
@RequestMapping("/api/v1/attachments")
@Tag(name = "Attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * Requirement 33.6: authentication is required, and the service checks the actor's access to
     * the owning record before any bytes are read. Roles are not fixed here because every role owns
     * some attachment type; the data-level check is the meaningful one.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download an attachment the caller may access")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        AttachmentDownload download = attachmentService.download(id);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.byteSize())
                .body(download.resource());
    }
}

package com.vendorsphere.common.attachment;

import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private static final String NOT_FOUND_MESSAGE = "Attachment not found";

    private final AttachmentRepository attachmentRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AttachmentStorage storage;
    private final ObjectProvider<AttachmentOwnerAccessPolicy> accessPolicies;

    public AttachmentServiceImpl(
            AttachmentRepository attachmentRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            AttachmentStorage storage,
            ObjectProvider<AttachmentOwnerAccessPolicy> accessPolicies
    ) {
        this.attachmentRepository = attachmentRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.accessPolicies = accessPolicies;
    }

    @Override
    @Transactional
    public AttachmentResponse upload(
            AttachmentOwnerType ownerType, UUID ownerId, MultipartFile file) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        assertOwnerAccessible(ownerType, ownerId);

        // Every gate runs before a byte is written, so a rejected upload leaves no file behind.
        assertPresent(file);
        String contentType = assertAcceptedContentType(file);
        assertWithinSizeLimit(file);

        String storageReference = storage.store(file);

        Attachment attachment = new Attachment();
        attachment.setOrganization(organizationRepository.getReferenceById(organizationId));
        attachment.setOwnerType(ownerType);
        attachment.setOwnerId(ownerId);
        attachment.setOriginalFilename(AttachmentFilenames.sanitize(file.getOriginalFilename()));
        attachment.setContentType(contentType);
        attachment.setByteSize(file.getSize());
        attachment.setStorageReference(storageReference);
        attachment.setUploadedBy(userRepository.getReferenceById(actorId));

        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(AttachmentOwnerType ownerType, UUID ownerId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        assertOwnerAccessible(ownerType, ownerId);
        return attachmentRepository
                .findByOrganizationIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(
                        organizationId, ownerType, ownerId)
                .stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownload download(UUID attachmentId) {
        Attachment attachment = findInOrganization(attachmentId);
        assertOwnerAccessible(attachment.getOwnerType(), attachment.getOwnerId());
        return new AttachmentDownload(
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getByteSize(),
                storage.load(attachment.getStorageReference()));
    }

    @Override
    @Transactional
    public void delete(UUID attachmentId) {
        Attachment attachment = findInOrganization(attachmentId);
        assertOwnerAccessible(attachment.getOwnerType(), attachment.getOwnerId());
        String storageReference = attachment.getStorageReference();
        attachmentRepository.delete(attachment);
        storage.delete(storageReference);
    }

    private Attachment findInOrganization(UUID attachmentId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return attachmentRepository.findByIdAndOrganizationId(attachmentId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    private void assertOwnerAccessible(AttachmentOwnerType ownerType, UUID ownerId) {
        policyFor(ownerType).ifPresent(policy -> policy.assertAccessible(ownerId));
    }

    private Optional<AttachmentOwnerAccessPolicy> policyFor(AttachmentOwnerType ownerType) {
        return accessPolicies.stream()
                .filter(policy -> policy.ownerType() == ownerType)
                .findFirst();
    }

    private void assertPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required", HttpStatus.BAD_REQUEST);
        }
    }

    private String assertAcceptedContentType(MultipartFile file) {
        String declared = file.getContentType();
        String normalized = declared == null
                ? null
                : declared.split(";")[0].trim().toLowerCase(Locale.ROOT);

        if (normalized == null || !ACCEPTED_CONTENT_TYPES.contains(normalized)) {
            throw new BusinessException(
                    "Unsupported content type. Accepted content types: "
                            + String.join(", ", ACCEPTED_CONTENT_TYPES),
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return normalized;
    }

    private void assertWithinSizeLimit(MultipartFile file) {
        if (file.getSize() > MAX_BYTE_SIZE) {
            throw new BusinessException(SIZE_LIMIT_MESSAGE, HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }
}

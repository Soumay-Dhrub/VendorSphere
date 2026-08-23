package com.vendorsphere.common.attachment;

/**
 * Sanitizes the client-supplied file name before it is persisted as metadata.
 *
 * <p>The name never takes part in path resolution — storage references are random UUIDs
 * (Requirement 33.5) — but it is untrusted input that ends up in a database column and in a
 * {@code Content-Disposition} header, so directory separators, control characters and pure-dot
 * names are stripped here rather than trusted downstream.
 */
public final class AttachmentFilenames {

    /** Used when nothing usable survives sanitization. */
    static final String FALLBACK = "upload";

    /** {@code attachments.original_filename} is {@code VARCHAR(255)}. */
    private static final int MAX_LENGTH = 255;

    private AttachmentFilenames() {
    }

    public static String sanitize(String rawFilename) {
        if (rawFilename == null) {
            return FALLBACK;
        }

        String candidate = rawFilename.trim();
        int lastSeparator = Math.max(candidate.lastIndexOf('/'), candidate.lastIndexOf('\\'));
        if (lastSeparator >= 0) {
            candidate = candidate.substring(lastSeparator + 1);
        }

        candidate = candidate.replaceAll("\\p{Cntrl}", "").trim();

        if (candidate.isEmpty() || candidate.chars().allMatch(c -> c == '.')) {
            return FALLBACK;
        }

        return candidate.length() > MAX_LENGTH ? candidate.substring(0, MAX_LENGTH) : candidate;
    }
}

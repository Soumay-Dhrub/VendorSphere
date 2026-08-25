package com.vendorsphere.common.attachment;

public final class AttachmentFilenames {

    static final String FALLBACK = "upload";

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

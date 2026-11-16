package kitnapper.service;

/**
 * User-facing photo messages, kept out of {@link ImageStoreImpl} (the same
 * pattern as {@code LoanMessages} and {@code PenaltyMessages}). Controllers
 * translate the thrown exceptions into dialog text; views never see them.
 */
final class ImageMessages {

    static final String UNREADABLE = "That file is not a readable image.";
    static final String TOO_LARGE = "That photo is larger than 20 MB.";
    static final String SOURCE_MISSING = "That file no longer exists.";
    static final String STAGED_MISSING = "The imported photo is no longer available.";
    static final String UNSAFE_NAME = "That photo file name is not allowed.";

    private ImageMessages() {
    }

    static String unsupportedType(String extension) {
        return extension.isEmpty()
                ? "That file has no image extension."
                : "Unsupported image type: ." + extension;
    }
}

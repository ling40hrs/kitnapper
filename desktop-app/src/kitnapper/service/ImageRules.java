package kitnapper.service;

import java.util.List;

/**
 * Every photo limit and layout constant in one place (design §12, D7):
 * the folder name, the accepted source extensions, the 20 MB cap, the
 * 1024 px downscale target, the two display sizes, and the JPEG quality.
 * {@link ImageStoreImpl} and the probes read these; nothing hard-codes them.
 */
public final class ImageRules {

    /** Folder beside the save file; never inside the repository (design §12). */
    public static final String FOLDER = "kitnapper-images";

    /** Prefix for an imported-but-unsaved photo, deleted by the sweep. */
    public static final String STAGING_PREFIX = "staging-";

    /** Stored photos are always JPEG, named after the equipment id. */
    public static final String OUTPUT_SUFFIX = ".jpg";

    /** Accepted source extensions, lower case, without the dot. */
    public static final List<String> EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "bmp");

    /** Largest accepted source file (design §12). */
    public static final long MAX_SOURCE_BYTES = 20L * 1024 * 1024;

    /** Longest edge of a stored photo (design §12). */
    public static final int MAX_EDGE_PX = 1024;

    /** Card thumbnail decode target (design §12). */
    public static final int CARD_EDGE_PX = 160;

    /** Preview-pane decode target (design §12). */
    public static final int PREVIEW_EDGE_PX = 640;

    /** Re-encode quality (design §12). */
    public static final float JPEG_QUALITY = 0.85f;

    private ImageRules() {
    }
}

package kitnapper.service;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Item-photo storage (design §12, D7, D22). One photo per item, always
 * JPEG, named {@code <equipmentId>.jpg} inside {@link ImageRules#FOLDER}
 * beside the save file. Imports stage a validated, downscaled copy first so
 * a cancelled dialog can never leave a file that looks like an item photo;
 * {@link #promote} turns a staged file into the item's photo.
 */
public interface ImageStore {

    /** The photo directory; it may not exist until the first import. */
    Path folder();

    /**
     * Validates, decodes, downscales and re-encodes a chosen file, writing a
     * staged copy.
     *
     * @param source   the picked file
     * @param matteRgb opaque background for transparent sources (D34)
     * @return the staged bare file name
     * @throws IllegalArgumentException when the extension, size, or contents
     *                                  are unacceptable (D16)
     */
    String stage(Path source, int matteRgb);

    /**
     * Moves a staged file to {@code <equipmentId>.jpg}, replacing any
     * previous photo.
     *
     * @return the stored bare file name
     * @throws IllegalArgumentException when the staged file is gone
     */
    String promote(String stagedName, String equipmentId);

    /**
     * The decoded photo, scaled so its longest edge is at most
     * {@code maxEdge}. Empty when the item has no photo, the file is gone,
     * or the file cannot be decoded — never an error surface (NFR-08).
     */
    Optional<BufferedImage> load(String photoFile, int maxEdge);

    /** Removes one photo; a missing file is not an error. */
    void delete(String photoFile);

    /** Empties the folder; used by the demo reset (D11). */
    void deleteAll();

    /**
     * Load-time cleanup (D11): deletes staged files and every photo whose
     * name does not match a live equipment id. Other files are untouched.
     */
    void sweep(Collection<String> equipmentIds);
}

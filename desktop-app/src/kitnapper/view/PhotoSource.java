package kitnapper.view;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * View-declared photo loader (plan §5.7). The controller implements it over
 * {@code ImageStore}, so cards and previews can decode photos without a view
 * importing the service layer. {@link PhotoTile} calls it off the EDT through
 * {@link SkeletonGate}; an empty result means "no photo", never an error.
 */
public interface PhotoSource {

    /** A photo source for screens built without one: every item falls back. */
    PhotoSource NONE = (photoFile, maxEdge) -> Optional.empty();

    /**
     * @param photoFile bare file name from the equipment item
     * @param maxEdge   longest-edge target in pixels
     * @return the decoded image, or empty when it is missing or unreadable
     */
    Optional<BufferedImage> load(String photoFile, int maxEdge);
}

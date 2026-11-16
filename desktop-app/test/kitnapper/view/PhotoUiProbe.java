package kitnapper.view;

import javax.swing.SwingUtilities;

/**
 * Photo display probe (M5 WP2, plan §9.3): mounts real {@link PhotoTile}s and
 * a {@link SkeletonPanel} in a frame and checks the photo, fallback, skeleton,
 * failure, cache, and reduced-motion paths. Needs a display (xvfb-run).
 */
public final class PhotoUiProbe {

    private PhotoUiProbe() {
    }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        PhotoTileChecks checks = new PhotoTileChecks();
        SwingUtilities.invokeAndWait(checks::build);
        try {
            checks.run();
        } finally {
            SwingUtilities.invokeAndWait(checks::dispose);
        }
        UiProbeSupport.finish();
    }
}

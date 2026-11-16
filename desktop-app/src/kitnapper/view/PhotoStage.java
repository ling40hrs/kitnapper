package kitnapper.view;

/**
 * Outcome of a photo import attempt (M5 WP4): the staged file name on success,
 * or the message the well shows inline. Declared in the view because
 * {@link InventoryScreen.Actions} exposes it to the dialog.
 *
 * @param stagedName bare staged file name, or null when the import failed
 * @param error      the user-facing reason, or null on success
 */
public record PhotoStage(String stagedName, String error) {

    public static PhotoStage staged(String stagedName) {
        return new PhotoStage(stagedName, null);
    }

    public static PhotoStage failed(String error) {
        return new PhotoStage(null, error);
    }

    public boolean ok() {
        return stagedName != null;
    }
}

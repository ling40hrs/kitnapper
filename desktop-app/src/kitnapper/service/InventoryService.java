package kitnapper.service;

/**
 * Inventory mutations (REQ-010, FR-10) for the manager screens. Every method
 * re-checks the session role (INV-3, D6) and rejects illegal transitions
 * before calling the entity methods (INV-1, INV-5). Failures are thrown and
 * translated by the controller (D16); the store is saved once per mutation.
 */
public interface InventoryService {

    /** The id the next added item will receive; no reservation is made. */
    String peekNextId();

    /**
     * Adds an item, kind fixed by the draft's category (D19).
     *
     * @throws IllegalArgumentException blank name/serial or an invalid value
     * @throws IllegalStateException    the session is not a manager
     */
    void add(ItemDraft draft);

    /**
     * Edits an item's mutable fields. The concrete kind never changes, even
     * when the category does; only the kind's own extra field is applied.
     *
     * @throws IllegalArgumentException invalid fields
     * @throws IllegalStateException    missing item, retired item, or wrong role
     */
    void update(String equipmentId, ItemDraft draft);

    /**
     * Promotes a staged import to the item's photo and records the name.
     *
     * @throws IllegalArgumentException the staged file is gone
     * @throws IllegalStateException    missing item or wrong role
     */
    void setPhoto(String equipmentId, String stagedName);

    /** Clears the photo reference and deletes the stored file. */
    void removePhoto(String equipmentId);

    /** AVAILABLE or LOANED to UNDER_MAINTENANCE (D15 keeps returns intact). */
    void flagMaintenance(String equipmentId);

    /**
     * UNDER_MAINTENANCE back to AVAILABLE. Rejected while the item is still
     * out on loan, so an out item can never become borrowable again.
     */
    void clearMaintenance(String equipmentId);

    /**
     * Retires an item (terminal). Rejected while the item is out on loan, so
     * the open loan can still be returned.
     */
    void decommission(String equipmentId);

    /** Wipes uploaded photos and reseeds the demo dataset relative to today (D4). */
    void resetDemoData();
}

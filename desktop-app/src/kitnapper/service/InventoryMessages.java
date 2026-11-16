package kitnapper.service;

/**
 * User-facing inventory messages, kept out of the service algorithm the same
 * way {@code LoanMessages} and {@code PenaltyMessages} are. Controllers turn
 * these into dialog or toast text; nothing here reaches a view directly.
 */
final class InventoryMessages {

    static final String MANAGER_ONLY = "Only managers can change the inventory.";
    static final String ITEM_MISSING = "That item is no longer in the inventory.";
    static final String READ_ONLY = "Decommissioned items are read-only.";
    static final String NAME_REQUIRED = "Name is required.";
    static final String SERIAL_REQUIRED = "Serial number is required.";
    static final String VALUE_INVALID = "Replacement value must be zero or more.";
    static final String INTERVAL_INVALID = "Maintenance interval must be zero or more.";
    static final String FLAG_BLOCKED =
            "Only available or loaned items can be flagged for maintenance.";
    static final String CLEAR_BLOCKED = "Only items flagged for maintenance can be cleared.";
    static final String DECOMMISSION_BLOCKED =
            "Only available items, or items already flagged for maintenance, can be decommissioned.";
    static final String ON_LOAN = "This item is still on loan.";

    private InventoryMessages() {
    }
}

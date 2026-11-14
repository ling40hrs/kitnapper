package kitnapper.model;

/**
 * Lifecycle states of an item. Transitions are validated exclusively by the
 * Equipment transition methods (PRD 4.1); DECOMMISSIONED is terminal.
 */
public enum EquipmentStatus {
    AVAILABLE,
    LOANED,
    UNDER_MAINTENANCE,
    DECOMMISSIONED
}

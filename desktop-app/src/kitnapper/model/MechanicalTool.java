package kitnapper.model;

/**
 * Equipment specialization for mechanical gear; adds an optional
 * maintenance interval in days (implementation plan 2.1). The extra field
 * is independent of the category; the kind is fixed at construction.
 */
public final class MechanicalTool extends Equipment {

    private int maintenanceIntervalDays;

    public MechanicalTool(String id, String name, EquipmentCategory category,
                          String serialNumber, double replacementValue,
                          int maintenanceIntervalDays) {
        super(id, name, category, serialNumber, replacementValue);
        updateMaintenanceInterval(maintenanceIntervalDays);
    }

    public int getMaintenanceIntervalDays() {
        return maintenanceIntervalDays;
    }

    /** Zero means "no interval on record"; negative values are rejected. */
    public void updateMaintenanceInterval(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("maintenanceIntervalDays must not be negative");
        }
        this.maintenanceIntervalDays = days;
    }
}

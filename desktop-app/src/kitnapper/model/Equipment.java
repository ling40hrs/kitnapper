package kitnapper.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Abstract lendable item. The concrete subclasses ({@link DigitalEquipment},
 * {@link MechanicalTool}, {@link GeneralEquipment}) add optional fields; the
 * concrete kind is fixed at construction and is independent of the category.
 *
 * <p>Status changes happen only through the transition methods below, each of
 * which validates the legal transitions of PRD 4.1 (INV-1, INV-5). There is
 * no public setter for the status, and DECOMMISSIONED is terminal.</p>
 */
public abstract class Equipment {

    private final String id;
    private String name;
    private EquipmentCategory category;
    private EquipmentStatus status;
    private String serialNumber;
    private double replacementValue;
    private String photoFile;

    /**
     * @param id               unique identifier, non-blank
     * @param name             display name, non-blank
     * @param category         gear category, non-null
     * @param serialNumber     manufacturer serial number, non-blank
     * @param replacementValue replacement cost in PHP, finite and non-negative
     */
    protected Equipment(String id, String name, EquipmentCategory category,
                        String serialNumber, double replacementValue) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.category = Objects.requireNonNull(category, "category");
        this.serialNumber = requireNonBlank(serialNumber, "serialNumber");
        this.replacementValue = requireNonNegative(replacementValue);
        this.status = EquipmentStatus.AVAILABLE;
        this.photoFile = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public double getReplacementValue() {
        return replacementValue;
    }

    /** Empty when the item has no photo; otherwise a bare file name (design 12). */
    public Optional<String> getPhotoFile() {
        return Optional.ofNullable(photoFile);
    }

    /** True only while AVAILABLE; loans are impossible otherwise (INV-1). */
    public boolean isBorrowable() {
        return status == EquipmentStatus.AVAILABLE;
    }

    /**
     * Edits the mutable details. The identifier, status and photo are not
     * touched here, and the concrete kind is never changed by edits.
     */
    public void updateDetails(String name, EquipmentCategory category,
                              String serialNumber, double replacementValue) {
        this.name = requireNonBlank(name, "name");
        this.category = Objects.requireNonNull(category, "category");
        this.serialNumber = requireNonBlank(serialNumber, "serialNumber");
        this.replacementValue = requireNonNegative(replacementValue);
    }

    /**
     * Replaces or clears the photo file name. Only a bare file name is
     * accepted (no path separators, no traversal); the file itself is
     * managed by ImageStore (design 12).
     */
    public void setPhotoFile(String photoFile) {
        if (photoFile == null) {
            this.photoFile = null;
            return;
        }
        String trimmed = photoFile.trim();
        if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains("\\")
                || trimmed.contains("..")) {
            throw new IllegalArgumentException("photoFile must be a plain file name");
        }
        this.photoFile = trimmed;
    }

    /** Return or repair complete: LOANED or UNDER_MAINTENANCE -> AVAILABLE. */
    public final void markAvailable() {
        if (status != EquipmentStatus.LOANED && status != EquipmentStatus.UNDER_MAINTENANCE) {
            throw new IllegalStateException("Cannot mark " + status + " equipment available");
        }
        status = EquipmentStatus.AVAILABLE;
    }

    /** Checkout: AVAILABLE -> LOANED. INV-1: every other state is rejected. */
    public final void markLoaned() {
        if (status != EquipmentStatus.AVAILABLE) {
            throw new IllegalStateException("Only AVAILABLE equipment can be loaned, was " + status);
        }
        status = EquipmentStatus.LOANED;
    }

    /** Manager flags for repair: AVAILABLE or LOANED -> UNDER_MAINTENANCE. */
    public final void markUnderMaintenance() {
        if (status != EquipmentStatus.AVAILABLE && status != EquipmentStatus.LOANED) {
            throw new IllegalStateException("Cannot flag " + status + " equipment for maintenance");
        }
        status = EquipmentStatus.UNDER_MAINTENANCE;
    }

    /** Retire: AVAILABLE or UNDER_MAINTENANCE -> DECOMMISSIONED (terminal). */
    public final void decommission() {
        if (status != EquipmentStatus.AVAILABLE && status != EquipmentStatus.UNDER_MAINTENANCE) {
            throw new IllegalStateException("Cannot decommission " + status + " equipment");
        }
        status = EquipmentStatus.DECOMMISSIONED;
    }

    static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static double requireNonNegative(double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("value must be a finite non-negative number");
        }
        return value;
    }
}

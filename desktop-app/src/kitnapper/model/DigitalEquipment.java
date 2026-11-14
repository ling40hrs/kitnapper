package kitnapper.model;

import java.util.Optional;

/**
 * Equipment specialization for digital gear; adds an optional warranty
 * contact (implementation plan 2.1). The extra field is independent of the
 * category; the concrete kind is fixed at construction.
 */
public final class DigitalEquipment extends Equipment {

    private String warrantyContact;

    public DigitalEquipment(String id, String name, EquipmentCategory category,
                            String serialNumber, double replacementValue,
                            String warrantyContact) {
        super(id, name, category, serialNumber, replacementValue);
        updateWarrantyContact(warrantyContact);
    }

    /** Empty when there is no contact on record. */
    public Optional<String> getWarrantyContact() {
        return Optional.ofNullable(warrantyContact);
    }

    /** Blank or null clears the contact. */
    public void updateWarrantyContact(String warrantyContact) {
        if (warrantyContact == null || warrantyContact.isBlank()) {
            this.warrantyContact = null;
        } else {
            this.warrantyContact = warrantyContact.trim();
        }
    }
}

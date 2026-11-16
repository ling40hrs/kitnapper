package kitnapper.view;

import kitnapper.model.EquipmentCategory;

/**
 * The add/edit form's field values as the dialog collects them (M5 WP4). The
 * numeric fields stay raw text so the dialog can report a format error inline
 * before anything reaches the service; the controller parses and re-validates
 * (D16), and the concrete kind is never a field (D19).
 *
 * @param name               display name
 * @param category           selected gear category
 * @param serialNumber       manufacturer serial
 * @param replacementValue   PHP as typed, e.g. {@code 145000} or {@code 145,000.00}
 * @param warrantyContact    digital-kind contact, blank for none
 * @param maintenanceInterval mechanical-kind interval in days, blank for none
 */
public record EquipmentForm(String name, EquipmentCategory category, String serialNumber,
                            String replacementValue, String warrantyContact,
                            String maintenanceInterval) {
}

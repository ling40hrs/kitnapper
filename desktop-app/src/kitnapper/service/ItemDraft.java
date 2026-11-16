package kitnapper.service;

import kitnapper.model.EquipmentCategory;

/**
 * Editable item fields as the add/edit dialog collects them (M5). The
 * concrete kind is not part of the draft: it is fixed at creation from the
 * category (D19) and preserved on every later edit. Both extra fields are
 * carried so one draft type serves all three kinds; the field that does not
 * belong to the item's kind is ignored by {@link InventoryServiceImpl}.
 *
 * @param name                    display name, non-blank
 * @param category                gear category, non-null
 * @param serialNumber            manufacturer serial, non-blank
 * @param replacementValue        PHP, finite and non-negative
 * @param warrantyContact         digital-kind contact, null/blank for none
 * @param maintenanceIntervalDays mechanical-kind interval, zero for none
 */
public record ItemDraft(String name, EquipmentCategory category, String serialNumber,
                        double replacementValue, String warrantyContact,
                        int maintenanceIntervalDays) {
}

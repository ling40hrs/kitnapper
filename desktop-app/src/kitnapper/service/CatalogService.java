package kitnapper.service;

import java.util.List;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;

/**
 * Read-only catalog queries (REQ-001). A blank or null query applies no text
 * filter; a null category or status applies no filter. Text matches the name
 * or serial case-insensitively, and results are sorted by name then ID.
 */
public interface CatalogService {

    /**
     * @param query    name/serial fragment, or null/blank for every item
     * @param category category filter, or null for any category
     * @param status   status filter, or null for any status
     * @return a fresh list sorted by case-insensitive name, then ID
     */
    List<Equipment> search(String query, EquipmentCategory category,
                           EquipmentStatus status);

    /** Every item; equivalent to {@code search(null, null, null)}. */
    List<Equipment> all();
}

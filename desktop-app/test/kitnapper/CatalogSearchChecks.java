package kitnapper;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import kitnapper.model.Equipment;
import kitnapper.service.CatalogService;

/**
 * Catalog search section of the catalog probe (plan §9.2 items 1–8): text,
 * case, serial, category, status, and composed filters over the deterministic
 * seed. Prints through the shared tally.
 */
final class CatalogSearchChecks {

    private CatalogSearchChecks() {
    }

    static void run(CatalogService catalog) {
        ProbeSupport.check("all-size-sorted", catalog.all().size() == 10
            && sortedByName(catalog.all())
            && sameIds(catalog.all(), catalog.search(null, null, null)));
        ProbeSupport.check("search-text",
            sameIds(catalog.search("sony", null, null), Set.of("e-001", "e-003")));
        ProbeSupport.check("search-case-insensitive",
            catalog.search("SONY", null, null).size() == 2);
        ProbeSupport.check("search-serial",
            sameIds(catalog.search("SN-EOS", null, null), Set.of("e-002")));
        ProbeSupport.check("search-category",
            sameIds(catalog.search(null, kitnapper.model.EquipmentCategory.CAMERA, null),
                Set.of("e-001", "e-002")));
        ProbeSupport.check("search-status",
            catalog.search(null, null, kitnapper.model.EquipmentStatus.AVAILABLE).size() == 7
            && catalog.search(null, null, kitnapper.model.EquipmentStatus.LOANED).size() == 3);
        ProbeSupport.check("search-composed",
            sameIds(catalog.search("sony", null, kitnapper.model.EquipmentStatus.LOANED),
                Set.of("e-001"))
            && sameIds(catalog.search("sony", null,
                    kitnapper.model.EquipmentStatus.AVAILABLE), Set.of("e-003")));
        ProbeSupport.check("search-empty", catalog.search("zzz", null, null).isEmpty()
            && catalog.search("   ", null, null).size() == 10);
    }

    private static boolean sortedByName(List<Equipment> items) {
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i - 1).getName().toLowerCase(Locale.ROOT)
                    .compareTo(items.get(i).getName().toLowerCase(Locale.ROOT)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameIds(List<Equipment> items, Set<String> expected) {
        Set<String> ids = new HashSet<>();
        for (Equipment item : items) {
            ids.add(item.getId());
        }
        return ids.equals(expected);
    }

    private static boolean sameIds(List<Equipment> items, List<Equipment> other) {
        Set<String> ids = new HashSet<>();
        Set<String> otherIds = new HashSet<>();
        for (Equipment item : items) {
            ids.add(item.getId());
        }
        for (Equipment item : other) {
            otherIds.add(item.getId());
        }
        return ids.equals(otherIds);
    }
}

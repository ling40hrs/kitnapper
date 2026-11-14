package kitnapper.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;

/**
 * In-memory catalog queries over the live {@link DataStore} lists. Filtering
 * never mutates the store; each call returns a fresh, sorted list.
 */
public final class CatalogServiceImpl implements CatalogService {

    private static final Comparator<Equipment> BY_NAME_THEN_ID =
            Comparator.comparing((Equipment item) -> item.getName().toLowerCase(Locale.ROOT))
                    .thenComparing(Equipment::getId);

    private final DataStore store;

    public CatalogServiceImpl(DataStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public List<Equipment> search(String query, EquipmentCategory category,
                                  EquipmentStatus status) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return store.equipment().stream()
                .filter(item -> matchesText(item, needle))
                .filter(item -> category == null || item.getCategory() == category)
                .filter(item -> status == null || item.getStatus() == status)
                .sorted(BY_NAME_THEN_ID)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Equipment> all() {
        return search(null, null, null);
    }

    private static boolean matchesText(Equipment item, String needle) {
        if (needle.isEmpty()) {
            return true;
        }
        return item.getName().toLowerCase(Locale.ROOT).contains(needle)
                || item.getSerialNumber().toLowerCase(Locale.ROOT).contains(needle);
    }
}

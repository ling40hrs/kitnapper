package kitnapper.controller;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import kitnapper.model.Equipment;
import kitnapper.view.InventoryScreen;
import kitnapper.view.PhotoSource;

/**
 * The inventory screen's view-declared callbacks (M5 WP3): query state and
 * mutations come from {@link InventoryController}, photos from the shared
 * {@link ItemPhotos} bridge, and the counts go to the shell's status sink.
 */
public final class InventoryActions implements InventoryScreen.Actions {

    private final InventoryController inventory;
    private final PhotoSource photos;
    private final BiConsumer<String, String> status;

    public InventoryActions(InventoryController inventory, PhotoSource photos,
                            BiConsumer<String, String> status) {
        this.inventory = inventory;
        this.photos = photos;
        this.status = status;
    }

    @Override
    public List<Equipment> search(String query) {
        inventory.setQuery(query);
        return inventory.results();
    }

    @Override
    public boolean isManager() {
        return inventory.isManager();
    }

    @Override
    public boolean onLoan(Equipment item) {
        return inventory.onLoan(item);
    }

    @Override
    public Optional<String> flagMaintenance(Equipment item) {
        return inventory.flagMaintenance(item);
    }

    @Override
    public Optional<String> clearMaintenance(Equipment item) {
        return inventory.clearMaintenance(item);
    }

    @Override
    public Optional<String> decommission(Equipment item) {
        return inventory.decommission(item);
    }

    @Override
    public PhotoSource photos() {
        return photos;
    }

    @Override
    public void setStatus(String left, String right) {
        status.accept(left, right);
    }
}

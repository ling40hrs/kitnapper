package kitnapper.service;

import java.util.Objects;
import java.util.function.Supplier;

import kitnapper.model.DigitalEquipment;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanRecord;
import kitnapper.model.LoanStatus;
import kitnapper.model.Manager;
import kitnapper.model.MechanicalTool;
import kitnapper.model.User;

/**
 * Inventory mutations behind the manager screens (REQ-010, FR-10). The
 * session comes from {@link AppContext} and is re-checked on every call
 * (INV-3, D6); transitions go through the entity methods only (INV-1,
 * INV-5) and each mutation saves once and notifies the context (D2).
 *
 * <p>Two loan-aware guards exist so inventory edits cannot corrupt lending:
 * clearing maintenance and decommissioning are both refused while the item
 * is still out, since the open loan must remain returnable.</p>
 */
public final class InventoryServiceImpl implements InventoryService {

    private final DataStore store;
    private final ImageStore images;
    private final Supplier<User> session;
    private final Runnable onChange;

    public InventoryServiceImpl(DataStore store, ImageStore images,
                                Supplier<User> session, Runnable onChange) {
        this.store = Objects.requireNonNull(store, "store");
        this.images = Objects.requireNonNull(images, "images");
        this.session = Objects.requireNonNull(session, "session");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    @Override
    public String peekNextId() {
        return Ids.nextEquipmentId(store.equipment());
    }

    @Override
    public void add(ItemDraft draft) {
        requireManager();
        ItemDraft valid = requireValid(draft);
        Equipment item = EquipmentFactory.create(peekNextId(), valid);
        store.equipment().add(item);
        persist();
    }

    @Override
    public void update(String equipmentId, ItemDraft draft) {
        requireManager();
        Equipment item = requireItem(equipmentId);
        if (item.getStatus() == EquipmentStatus.DECOMMISSIONED) {
            throw new IllegalStateException(InventoryMessages.READ_ONLY);
        }
        ItemDraft valid = requireValid(draft);
        item.updateDetails(valid.name(), valid.category(), valid.serialNumber(),
                valid.replacementValue());
        if (item instanceof DigitalEquipment digital) {
            digital.updateWarrantyContact(valid.warrantyContact());
        } else if (item instanceof MechanicalTool tool) {
            tool.updateMaintenanceInterval(valid.maintenanceIntervalDays());
        }
        persist();
    }

    @Override
    public void setPhoto(String equipmentId, String stagedName) {
        requireManager();
        Equipment item = requireItem(equipmentId);
        item.setPhotoFile(images.promote(stagedName, item.getId()));
        persist();
    }

    @Override
    public void removePhoto(String equipmentId) {
        requireManager();
        Equipment item = requireItem(equipmentId);
        item.getPhotoFile().ifPresent(images::delete);
        item.setPhotoFile(null);
        persist();
    }

    @Override
    public void flagMaintenance(String equipmentId) {
        requireManager();
        Equipment item = requireItem(equipmentId);
        transition(() -> item.markUnderMaintenance(), InventoryMessages.FLAG_BLOCKED);
        persist();
    }

    @Override
    public void clearMaintenance(String equipmentId) {
        requireManager();
        Equipment item = requireItem(equipmentId);
        requireNotOnLoan(equipmentId);
        transition(() -> item.markAvailable(), InventoryMessages.CLEAR_BLOCKED);
        persist();
    }

    @Override
    public void decommission(String equipmentId) {
        requireManager();
        Equipment item = requireItem(equipmentId);
        requireNotOnLoan(equipmentId);
        transition(() -> item.decommission(), InventoryMessages.DECOMMISSION_BLOCKED);
        persist();
    }

    @Override
    public void resetDemoData() {
        requireManager();
        images.deleteAll();
        store.resetToSeed();
        onChange.run();
    }

    private void requireManager() {
        if (!(session.get() instanceof Manager)) {
            throw new IllegalStateException(InventoryMessages.MANAGER_ONLY);
        }
    }

    private Equipment requireItem(String equipmentId) {
        for (Equipment item : store.equipment()) {
            if (item.getId().equals(equipmentId)) {
                return item;
            }
        }
        throw new IllegalStateException(InventoryMessages.ITEM_MISSING);
    }

    private void requireNotOnLoan(String equipmentId) {
        for (LoanRecord loan : store.loans()) {
            if (loan.getEquipmentId().equals(equipmentId)
                    && loan.getLoanStatus() != LoanStatus.RETURNED) {
                throw new IllegalStateException(InventoryMessages.ON_LOAN);
            }
        }
    }

    private static void transition(Runnable change, String message) {
        try {
            change.run();
        } catch (IllegalStateException blocked) {
            throw new IllegalStateException(message, blocked);
        }
    }

    private static ItemDraft requireValid(ItemDraft draft) {
        Objects.requireNonNull(draft, "draft");
        Objects.requireNonNull(draft.category(), "category");
        if (draft.name() == null || draft.name().isBlank()) {
            throw new IllegalArgumentException(InventoryMessages.NAME_REQUIRED);
        }
        if (draft.serialNumber() == null || draft.serialNumber().isBlank()) {
            throw new IllegalArgumentException(InventoryMessages.SERIAL_REQUIRED);
        }
        if (!Double.isFinite(draft.replacementValue()) || draft.replacementValue() < 0) {
            throw new IllegalArgumentException(InventoryMessages.VALUE_INVALID);
        }
        if (draft.maintenanceIntervalDays() < 0) {
            throw new IllegalArgumentException(InventoryMessages.INTERVAL_INVALID);
        }
        return draft;
    }

    private void persist() {
        store.save();
        onChange.run();
    }
}

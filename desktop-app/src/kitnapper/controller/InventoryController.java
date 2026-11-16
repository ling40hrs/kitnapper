package kitnapper.controller;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import kitnapper.model.Equipment;
import kitnapper.model.LoanRecord;
import kitnapper.model.LoanStatus;
import kitnapper.model.User;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.service.ItemDraft;
import kitnapper.view.EquipmentForm;
import kitnapper.view.PhotoStage;
import kitnapper.view.Theme;

/**
 * Inventory query state plus the mutation boundary (M5 WP3). Reads come from
 * {@code CatalogService} so the manager sees the same ordering as the catalog;
 * writes go to {@link kitnapper.service.InventoryService}, whose exceptions are
 * translated here so no view catches anything (D16, the
 * {@link BorrowingController} pattern).
 */
public final class InventoryController {

    private static final String SAVE_FAILED =
            "Could not save the change. Check the app folder and retry.";

    private final AppContext context;
    private String query = "";
    private Runnable onChanged;

    public InventoryController(AppContext context) {
        this.context = context;
        context.addChangeListener(this::notifyChanged);
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
    }

    /** Every item matching the omnibar text, name then id (text only, D-M5). */
    public List<Equipment> results() {
        return context.catalog().search(query, null, null);
    }

    public boolean isManager() {
        User user = context.currentUser();
        return user != null && user.getRole() == UserRole.MANAGER;
    }

    /** True while an unreturned loan references the item. */
    public boolean onLoan(Equipment item) {
        if (item == null) {
            return false;
        }
        for (LoanRecord loan : context.dataStore().loans()) {
            if (loan.getEquipmentId().equals(item.getId())
                    && loan.getLoanStatus() != LoanStatus.RETURNED) {
                return true;
            }
        }
        return false;
    }

    public Optional<String> flagMaintenance(Equipment item) {
        return run(() -> context.inventory().flagMaintenance(item.getId()),
            "Could not flag the item for maintenance.");
    }

    public Optional<String> clearMaintenance(Equipment item) {
        return run(() -> context.inventory().clearMaintenance(item.getId()),
            "Could not clear the maintenance flag.");
    }

    public Optional<String> decommission(Equipment item) {
        return run(() -> context.inventory().decommission(item.getId()),
            "Could not decommission the item.");
    }

    /** Adds a validated item, then attaches its staged photo when there is one. */
    public Optional<String> add(EquipmentForm form, String stagedPhoto) {
        try {
            ItemDraft draft = toDraft(form);
            String id = context.inventory().peekNextId();
            context.inventory().add(draft);
            if (stagedPhoto != null) {
                context.inventory().setPhoto(id, stagedPhoto);
            }
            return Optional.empty();
        } catch (UncheckedIOException saveFailure) {
            return Optional.of(SAVE_FAILED);
        } catch (RuntimeException failure) {
            return Optional.of(messageOr(failure, "Could not add the item."));
        }
    }

    /** Edits an item, then applies a staged photo or a removal when asked. */
    public Optional<String> update(String equipmentId, EquipmentForm form, String stagedPhoto,
                                   boolean removePhoto) {
        try {
            ItemDraft draft = toDraft(form);
            context.inventory().update(equipmentId, draft);
            if (stagedPhoto != null) {
                context.inventory().setPhoto(equipmentId, stagedPhoto);
            } else if (removePhoto) {
                context.inventory().removePhoto(equipmentId);
            }
            return Optional.empty();
        } catch (UncheckedIOException saveFailure) {
            return Optional.of(SAVE_FAILED);
        } catch (RuntimeException failure) {
            return Optional.of(messageOr(failure, "Could not update the item."));
        }
    }

    /** Imports a chosen file into the photo folder; the matte is the theme surface (D34). */
    public PhotoStage stagePhoto(Path source) {
        try {
            return PhotoStage.staged(context.images().stage(source, Theme.bgSurface.getRGB()));
        } catch (UncheckedIOException writeFailure) {
            return PhotoStage.failed(
                "Could not write the photo. Check the app folder and retry.");
        } catch (RuntimeException rejected) {
            return PhotoStage.failed(messageOr(rejected, "That photo could not be imported."));
        }
    }

    /** Drops a staged import that was never saved. */
    public void discardStaged(String stagedName) {
        context.images().delete(stagedName);
    }

    public void setOnChanged(Runnable listener) {
        this.onChanged = listener;
    }

    /** Parses the form's raw text into the service draft, with user-facing errors. */
    private static ItemDraft toDraft(EquipmentForm form) {
        return new ItemDraft(form.name(), form.category(), form.serialNumber(),
            parseValue(form.replacementValue()), form.warrantyContact(),
            parseInterval(form.maintenanceInterval()));
    }

    private static double parseValue(String raw) {
        String cleaned = raw == null ? "" : raw.replace("₱", "").replace(",", "").trim();
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(
                "Replacement value must be a number, zero or more.");
        }
    }

    private static int parseInterval(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(
                "Maintenance interval must be a whole number of days.");
        }
    }

    private static String messageOr(RuntimeException failure, String fallback) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }

    private Optional<String> run(Runnable action, String fallback) {
        try {
            action.run();
            return Optional.empty();
        } catch (UncheckedIOException saveFailure) {
            return Optional.of(SAVE_FAILED);
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            return Optional.of(message == null || message.isBlank() ? fallback : message);
        }
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}

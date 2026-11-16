package kitnapper.view;

import java.awt.Window;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import kitnapper.model.Equipment;

/**
 * The add/edit item dialog (design §5 "Inventory", §8.2). Fields come from
 * {@link EquipmentFields}; the photo well imports on file choice in a worker
 * (the dialog's confirm handler is synchronous on the EDT), staging the file
 * so a cancelled dialog leaves nothing behind. Saving validates inline,
 * calls the controller, and keeps the dialog open with the service's message
 * when the save is rejected.
 */
public final class EquipmentDialog {

    private final InventoryScreen.Actions actions;
    private final Equipment existing;
    private final EquipmentFields fields;
    private final PhotoWell well;
    private String stagedName;
    private boolean removeRequested;

    private EquipmentDialog(Equipment existing, InventoryScreen.Actions actions) {
        this.existing = existing;
        this.actions = actions;
        this.fields = new EquipmentFields(existing);
        this.well = new PhotoWell(actions.photos(), new PhotoWell.Listener() {
            public void photoChosen(Path source) {
                importPhoto(source);
            }

            public void photoRemoved() {
                requestRemove();
            }
        });
    }

    /** Shows the dialog; true when the item was saved. */
    public static boolean show(Window owner, Equipment existing,
                               InventoryScreen.Actions actions) {
        return new EquipmentDialog(existing, actions).open(owner);
    }

    private boolean open(Window owner) {
        String title = existing == null ? "Add item" : "Edit item";
        boolean saved = Dialogs.showForm(owner, title, body(), "Save", false, this::save);
        if (!saved && stagedName != null) {
            actions.discardStaged(stagedName);
        }
        return saved;
    }

    private JPanel body() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(fields);
        body.add(Box.createVerticalStrut(4));
        body.add(well);
        fields.setOnCategoryChanged(this::showCurrentPhoto);
        showCurrentPhoto();
        return body;
    }

    /** Reflects what saving would keep: staged, existing, or no photo. */
    private void showCurrentPhoto() {
        String file = currentFile();
        well.showFile(file, fields.selectedCategory(), label());
        well.setRemoveEnabled(file != null);
        if (stagedName != null) {
            well.setStatus("Photo ready. Save to keep it.", false);
        } else if (removeRequested) {
            well.setStatus("Photo will be removed on save.", false);
        } else {
            well.setStatus("", false);
        }
    }

    private void importPhoto(Path source) {
        if (currentFile() != null && !confirmReplace()) {
            return;
        }
        well.setBusy(true);
        well.setStatus("Importing photo…", false);
        new SwingWorker<PhotoStage, Void>() {
            @Override
            protected PhotoStage doInBackground() {
                return actions.stagePhoto(source);
            }

            @Override
            protected void done() {
                PhotoStage stage;
                try {
                    stage = get();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException | CancellationException failed) {
                    stage = null;
                }
                well.setBusy(false);
                if (stage != null && stage.ok()) {
                    stagedName = stage.stagedName();
                    removeRequested = false;
                    showCurrentPhoto();
                    return;
                }
                showCurrentPhoto();
                well.setStatus(stage == null
                    ? "That photo could not be imported." : stage.error(), true);
            }
        }.execute();
    }

    private void requestRemove() {
        if (!Dialogs.confirmDestructive(owner(), "Remove photo?",
                "The photo is deleted when you save.", "Remove")) {
            return;
        }
        if (stagedName != null) {
            actions.discardStaged(stagedName);
            stagedName = null;
        }
        removeRequested = existing != null && existing.getPhotoFile().isPresent();
        well.showFile(null, fields.selectedCategory(), null);
        well.setRemoveEnabled(false);
        well.setStatus(removeRequested ? "Photo will be removed on save." : "", false);
    }

    private boolean confirmReplace() {
        return Dialogs.confirmDestructive(owner(), "Replace photo?",
            "The current photo is replaced when you save.", "Replace");
    }

    private Optional<String> save() {
        Optional<String> invalid = fields.validationError();
        if (invalid.isPresent()) {
            return invalid;
        }
        EquipmentForm form = fields.form();
        Optional<String> failure = existing == null
            ? actions.add(form, stagedName)
            : actions.update(existing.getId(), form, stagedName, removeRequested);
        if (failure.isEmpty()) {
            stagedName = null;
        }
        return failure;
    }

    private String currentFile() {
        if (stagedName != null) {
            return stagedName;
        }
        return removeRequested || existing == null ? null : existing.getPhotoFile().orElse(null);
    }

    private String label() {
        return existing == null ? null : existing.getName();
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(well);
    }
}

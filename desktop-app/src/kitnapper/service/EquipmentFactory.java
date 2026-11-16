package kitnapper.service;

import java.util.Objects;

import kitnapper.model.DigitalEquipment;
import kitnapper.model.Equipment;
import kitnapper.model.GeneralEquipment;
import kitnapper.model.MechanicalTool;

/**
 * Creates the concrete equipment kind for a new item (D19): the category
 * picks the kind once, at creation, and nothing afterwards can change it —
 * an edit that changes the category keeps the original class.
 */
public final class EquipmentFactory {

    private EquipmentFactory() {
    }

    /**
     * @param id    the system-generated identifier (D22)
     * @param draft validated item fields
     * @return a new item in {@code AVAILABLE}, kind fixed by the category
     */
    public static Equipment create(String id, ItemDraft draft) {
        Objects.requireNonNull(draft, "draft");
        Objects.requireNonNull(draft.category(), "category");
        return switch (draft.category()) {
            case CAMERA, LENS, LAPTOP, AUDIO -> new DigitalEquipment(id, draft.name(),
                    draft.category(), draft.serialNumber(), draft.replacementValue(),
                    draft.warrantyContact());
            case TOOL -> new MechanicalTool(id, draft.name(), draft.category(),
                    draft.serialNumber(), draft.replacementValue(),
                    draft.maintenanceIntervalDays());
            case OTHER -> new GeneralEquipment(id, draft.name(), draft.category(),
                    draft.serialNumber(), draft.replacementValue());
        };
    }
}

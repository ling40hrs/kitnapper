package kitnapper.service.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import kitnapper.model.DigitalEquipment;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.GeneralEquipment;
import kitnapper.model.MechanicalTool;

/**
 * Maps every concrete equipment kind to JSON and back (D14). The kind
 * discriminator selects the subclass on read; the persisted status is
 * restored by replaying the legal transition methods from the constructed
 * AVAILABLE state, then the optional photo file name is set (D8).
 * Per-kind fields are written only for their kind (D14).
 */
public final class EquipmentMapper {

    public Map<String, Object> toDocument(Equipment equipment) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", equipment.getId());
        doc.put("name", equipment.getName());
        doc.put("category", equipment.getCategory().name());
        doc.put("status", equipment.getStatus().name());
        doc.put("serialNumber", equipment.getSerialNumber());
        doc.put("replacementValue", equipment.getReplacementValue());
        if (equipment instanceof DigitalEquipment digital) {
            doc.put("kind", "DIGITAL");
            doc.put("warrantyContact", digital.getWarrantyContact().orElse(null));
        } else if (equipment instanceof MechanicalTool tool) {
            doc.put("kind", "MECHANICAL");
            doc.put("maintenanceIntervalDays", tool.getMaintenanceIntervalDays());
        } else if (equipment instanceof GeneralEquipment) {
            doc.put("kind", "BASE");
        } else {
            throw new IllegalArgumentException("Unsupported equipment kind: " + equipment.getClass());
        }
        doc.put("photoFile", equipment.getPhotoFile().orElse(null));
        return doc;
    }

    public Equipment fromDocument(Object value) {
        Map<String, Object> doc = Json.asObject(value, "equipment");
        EquipmentCategory category = parseCategory(Json.requireString(doc, "category"));
        String kind = Json.requireString(doc, "kind");
        Equipment equipment = switch (kind) {
            case "DIGITAL" -> new DigitalEquipment(
                    Json.requireString(doc, "id"),
                    Json.requireString(doc, "name"),
                    category,
                    Json.requireString(doc, "serialNumber"),
                    Json.requireDouble(doc, "replacementValue"),
                    Json.optString(doc, "warrantyContact"));
            case "MECHANICAL" -> new MechanicalTool(
                    Json.requireString(doc, "id"),
                    Json.requireString(doc, "name"),
                    category,
                    Json.requireString(doc, "serialNumber"),
                    Json.requireDouble(doc, "replacementValue"),
                    Json.requireInt(doc, "maintenanceIntervalDays"));
            case "BASE" -> new GeneralEquipment(
                    Json.requireString(doc, "id"),
                    Json.requireString(doc, "name"),
                    category,
                    Json.requireString(doc, "serialNumber"),
                    Json.requireDouble(doc, "replacementValue"));
            default -> throw new IllegalArgumentException("Unknown kind: " + kind);
        };
        replayStatus(equipment, Json.requireString(doc, "status"));
        equipment.setPhotoFile(Json.optString(doc, "photoFile"));
        return equipment;
    }

    /** Every persisted status is reachable from the constructed AVAILABLE state. */
    private static void replayStatus(Equipment equipment, String status) {
        switch (status) {
            case "AVAILABLE" -> {
            }
            case "LOANED" -> equipment.markLoaned();
            case "UNDER_MAINTENANCE" -> equipment.markUnderMaintenance();
            case "DECOMMISSIONED" -> equipment.decommission();
            default -> throw new IllegalArgumentException("Unknown equipment status: " + status);
        }
    }

    private static EquipmentCategory parseCategory(String raw) {
        try {
            return EquipmentCategory.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown category: " + raw);
        }
    }
}

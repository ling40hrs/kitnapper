package kitnapper.model;

/**
 * Concrete Equipment without subclass fields. Exists so the JSON
 * {@code kind: "BASE"} discriminator can restore a plain item while
 * {@link Equipment} stays abstract (decision D14).
 */
public final class GeneralEquipment extends Equipment {

    public GeneralEquipment(String id, String name, EquipmentCategory category,
                            String serialNumber, double replacementValue) {
        super(id, name, category, serialNumber, replacementValue);
    }
}

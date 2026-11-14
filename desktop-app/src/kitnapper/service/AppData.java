package kitnapper.service;

import java.util.ArrayList;
import java.util.List;

import kitnapper.model.Equipment;
import kitnapper.model.LoanRecord;
import kitnapper.model.Penalty;
import kitnapper.model.User;

/**
 * The in-memory aggregate of the whole dataset (implementation plan D17):
 * four live lists that services mutate in place through the entities' own
 * methods, after which {@code DataStore.save()} persists them.
 *
 * <p>The accessors deliberately return the live lists, not copies: adding
 * a loan or penalty is a list mutation, while entity edits go through the
 * entities themselves (INV-5). A mapper fills a fresh {@code AppData} and
 * it is swapped in only when the save file loaded completely and validly
 * (D18).</p>
 */
public final class AppData {

    private final List<User> users = new ArrayList<>();
    private final List<Equipment> equipment = new ArrayList<>();
    private final List<LoanRecord> loans = new ArrayList<>();
    private final List<Penalty> penalties = new ArrayList<>();

    /** All accounts; live and mutable. */
    public List<User> users() {
        return users;
    }

    /** All equipment items; live and mutable. */
    public List<Equipment> equipment() {
        return equipment;
    }

    /** All loan records; live and mutable. */
    public List<LoanRecord> loans() {
        return loans;
    }

    /** All penalties; live and mutable. */
    public List<Penalty> penalties() {
        return penalties;
    }
}

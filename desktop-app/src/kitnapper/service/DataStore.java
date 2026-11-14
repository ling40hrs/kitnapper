package kitnapper.service;

import java.util.List;

import kitnapper.model.Equipment;
import kitnapper.model.LoanRecord;
import kitnapper.model.Penalty;
import kitnapper.model.User;

/**
 * Persistence boundary (REQ-012, D17): services mutate the aggregate and
 * call {@link #save()} last, so every mutation reaches the JSON save file
 * through this single choke point.
 */
public interface DataStore {

    /**
     * Loads the save file; on a missing, unparseable, or semantically
     * corrupt file it seeds fresh demo data and saves it immediately
     * (NFR-07, D18). Never throws for a bad file.
     */
    AppData load();

    /** Persists the current aggregate atomically (tmp file + move). */
    void save();

    /** Regenerates the seed dataset relative to today (FR-12, D4). */
    void resetToSeed();

    /** All accounts; live list shared with the aggregate. */
    List<User> users();

    /** All equipment items; live list shared with the aggregate. */
    List<Equipment> equipment();

    /** All loan records; live list shared with the aggregate. */
    List<LoanRecord> loans();

    /** All penalties; live list shared with the aggregate. */
    List<Penalty> penalties();
}
